from fastapi import File, FastAPI, UploadFile, Depends, HTTPException, Header, status
import firebase_admin
from firebase_admin import credentials, auth, firestore
import os

app = FastAPI(title="NMS-AI Backend")

# ---- Firebase Admin init ON STARTUP (critical) ----
if not firebase_admin._apps:
    cred = credentials.Certificate(
        "/app/keys/alzheimer-s-detection-system-firebase-adminsdk-fbsvc-51ff29d549.json"
    )
    firebase_admin.initialize_app(cred)

db = firestore.client()


def get_firestore():
    return db


def optional_auth(authorization: str = Header(None)):
    if os.getenv("TEST_MODE") == "true":
        return {"uid": "test-user"}

    if authorization is None:
        raise HTTPException(status_code=401, detail="Missing Authorization header")

    if not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Invalid Authorization header")

    id_token = authorization.split(" ", 1)[1]
    try:
        return auth.verify_id_token(id_token)
    except Exception as e:
        raise HTTPException(status_code=401, detail=str(e))


async def verify_firebase_token(authorization: str = Header(...)):
    if not authorization.startswith("Bearer "):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid Authorization header format"
        )

    id_token = authorization.split(" ", 1)[1]
    try:
        return auth.verify_id_token(id_token)
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail=str(e),
        )


@app.get("/")
def root():
    return {"message": "Welcome to NeuroMind System AI Backend"}


@app.post("/predict/audio")
async def predict_audio(
    file: UploadFile = File(...),
    current_user: dict = Depends(verify_firebase_token)
):
    from predict import predict_from_audio

    save_path = f"/tmp/{file.filename}"
    with open(save_path, "wb") as f:
        f.write(await file.read())

    score = predict_from_audio(save_path)

    uid = current_user["uid"]
    get_firestore().collection("tests").document(uid).collection("audioTests").add({
        "filename": file.filename,
        "risk_score": float(score),
        "created_at": firestore.SERVER_TIMESTAMP,
    })

    return {"risk_score": score}


@app.get("/news/patient")
async def patient_news(current_user: dict = Depends(verify_firebase_token)):
    try:
        from ai_agents import patient_news_crew
        rss = "https://news.google.com/rss/search?q=dementia&hl=en-IE&gl=IE&ceid=IE:en"
        result = patient_news_crew.kickoff(inputs={"rss": rss})
        return {"news": result}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/news/doctor")
async def doctor_news(current_user: dict = Depends(verify_firebase_token)):
    try:
        from ai_agents import doctor_news_crew
        rss = "https://news.google.com/rss/search?q=alzheimers+research&hl=en"
        result = doctor_news_crew.kickoff(inputs={"rss": rss})
        return {"news": result}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


def average_from_collection(col_ref, field_name):
    docs = col_ref.stream()
    values = [
        d.to_dict().get(field_name)
        for d in docs
        if d.to_dict().get(field_name) is not None
    ]
    return sum(values) / len(values) if values else 0.0


@app.get("/risk/summary")
async def risk_summary(current_user: dict = Depends(optional_auth)):
    from ai_agents import risk_crew

    uid = current_user["uid"]
    base = get_firestore().collection("tests").document(uid)

    cognitive_avg = average_from_collection(base.collection("cognitiveTests"), "score")
    lifestyle_avg = average_from_collection(base.collection("lifestyleTests"), "score")
    audio_avg = average_from_collection(base.collection("audioTests"), "risk_score")

    total_risk = (0.60 * cognitive_avg + 0.25 * lifestyle_avg + 0.15 * audio_avg)

    label = (
        "HIGH" if total_risk >= 0.65 else
        "MODERATE" if total_risk >= 0.40 else
        "LOW"
    )

    risk_data = {
        "total": round(total_risk, 3),
        "label": label,
        "breakdown": {
            "cognitive": round(cognitive_avg, 3),
            "lifestyle": round(lifestyle_avg, 3),
            "audio": round(audio_avg, 3),
        }
    }

    explanation = risk_crew.kickoff(inputs={"risk": risk_data})

    return {"risk": risk_data, "explanation": explanation}
