import os
import feedparser
import json
from crewai import Agent, Crew, Task, LLM
from crewai.tools import tool

# os.environ["CREWAI_LLM_PROVIDER"] = "google"
# gemini_llm = LLM(
#     model="gemini/gemini-1.5-flash",
#     api_key=os.environ.get("GEMINI_API_KEY")
# )
# deepseek_llm = LLM(
#     model="deepseek/deepseek-chat",
#     api_key=os.getenv("DEEPSEEK_API_KEY"),
#     base_url="https://api.deepseek.com"
# )
ollama_llm = LLM(
    model="ollama/llama3",
    base_url="http://localhost:11434"
    #,base_url="http://host.docker.internal:11434"
)


@tool("fetch_news_rss")
def fetch_news_rss_tool(rss_url: str) -> str:
    """Fetch recent dementia news from an RSS feed."""
    feed = feedparser.parse(rss_url)
    items = [{"title": e.get("title"), "link": e.get("link"), "summary": e.get("summary")} for e in feed.entries[:5]]
    return json.dumps(items)

@tool("speech_risk_model")
def speech_risk_model(audio_path: str) -> str:
    """
        Use the trained TensorFlow speech model to estimate dementia risk
        from an audio file.

        Args:
            audio_path: Path to a .wav file on disk.

        Returns:
            A text summary containing the risk score and label.
        """
    from predict import predict_from_audio
    score = predict_from_audio(audio_path)
    label = "HIGH" if score >= 0.5 else "LOW"
    return f"Speech model risk score: {score:.3f} ({label} risk, threshold=0.5)."


@tool("interpret_combined_risk")
def interpret_combined_risk(risk: dict) -> str:
    """
    Interpret combined dementia risk scores and explain
    how each test category contributed.

    Args:
        risk: Dictionary with total risk, label, and breakdown.

    Returns:
        A structured explanation of dementia risk.
    """

    total = risk["total"]
    label = risk["label"]

    cog = risk["breakdown"]["cognitive"]
    life = risk["breakdown"]["lifestyle"]
    audio = risk["breakdown"]["audio"]

    return f"""
Overall dementia risk classification: {label}.

Your overall risk score is {total:.2f} (scale 0–1).

Contribution analysis:
- Cognitive assessments had the strongest influence ({cog:.2f}). These tests
  evaluate memory, reasoning, and attention, which are key indicators of
  cognitive decline.

- Lifestyle factors contributed moderately ({life:.2f}). These include sleep,
  physical activity, and daily habits, which are known to affect long-term
  brain health.

- Speech analysis contributed a smaller portion ({audio:.2f}). Speech-based
  markers can reflect subtle cognitive changes but are used as a supporting
  signal rather than a primary indicator.

This result is not a diagnosis. It is intended as a screening tool to help
identify potential risk and guide further clinical evaluation.
"""

risk_agent = Agent(
    name="CombinedRiskAgent",
    role="Clinical Risk Interpreter",
    goal="Explain combined dementia risk based on multiple screening scores",
    backstory=(
        "A clinical decision-support assistant trained to interpret "
        "multimodal screening results and explain how different factors "
        "contribute to dementia risk."
    ),
    tools=[interpret_combined_risk],
    llm=ollama_llm
)

formatter_agent = Agent(
    name="FormatterAgent",
    role="Formatter",
    goal="Format model predictions into readable medical reports",
    backstory="Converts AI model output into readable summaries for patients",
    llm=ollama_llm
)

patient_researcher_agent = Agent(
    name="PatientResearcherAgent",
    role="News Agent",
    goal="Summarise dementia news for patients using ChatGPT API",
    backstory="Scans dementia-related news and rewrites into simple text for patients",
    llm=ollama_llm,
    tools=[fetch_news_rss_tool]
)
medical_researcher_agent = Agent(
    name="MedicalResearcherAgent",
    role="Clinical Analyst",
    goal="Find latest clinical breakthroughs and drug trials for Alzheimer's.",
    backstory="A medical professional trained to find peer-reviewed research for doctors.",
    llm=ollama_llm,
    tools=[fetch_news_rss_tool]
)

patient_news_task = Task(
    description="Use the tool to fetch news from {rss_url} and create a simple summary for a patient.",
    expected_output="A 3-paragraph friendly news update.",
    agent=patient_researcher_agent
)

medical_news_task = Task(
    description="Use the tool to fetch news from {rss_url} and create a technical brief for a doctor.",
    expected_output="A bulleted list of clinical findings and trial links.",
    agent=medical_researcher_agent
)
risk_task = Task(
            description=(
                "Explain the patient's dementia risk based on the provided data:\n\n{risk}"
            ),
            expected_output=(
                "A clear, patient-friendly explanation of dementia risk and how "
                "each test category contributed."
            ),
            agent=risk_agent
        )

patient_news_crew = Crew(
    agents=[patient_researcher_agent],
    tasks=[patient_news_task],
)

doctor_news_crew = Crew(
    agents=[medical_researcher_agent],
    tasks=[medical_news_task],
)

risk_crew = Crew(
agents=[risk_agent],
    tasks=[risk_task]
)