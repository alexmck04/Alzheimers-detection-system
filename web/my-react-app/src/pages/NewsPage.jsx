import { useEffect, useState } from "react";

export default function NewsPage() {
  const [articles, setArticles] = useState([]);
  const [message, setMessage] = useState("");

  const fetchNews = async () => {
    try {
      const res = await fetch(
        `https://newsapi.org/v2/everything?q=alzheimers OR dementia OR "brain health" OR neurology
         &sortBy=publishedAt&language=en&apiKey=${import.meta.env.VITE_NEWS_API_KEY}`
      );

      const data = await res.json();

      if (data.articles && data.articles.length > 0) {
        const filtered = data.articles.filter((a) =>
          (a.title + " " + a.description)
            .toLowerCase()
            .match(/alz|dementia|memory|cognitive|brain|neuro/)
        );

        if (filtered.length === 0) {
          setMessage("No Alzheimer-related news found.");
        } else {
          setMessage("");
        }

        setArticles(filtered.slice(0, 12));
      } else {
        setMessage("No relevant news found.");
      }
    } catch (err) {
      console.error(err);
      setMessage("Failed to load news.");
    }
  };

  useEffect(() => {
    fetchNews();
  }, []);

  return (
    <div className="dashboard fade-in">

      <h2 className="page-title">Latest Alzheimer’s & Brain Health News</h2>

      {message && articles.length === 0 && (
        <p className="auth-message">{message}</p>
      )}

      <div className="news-grid">
        {articles.map((a, idx) => (
          <div key={idx} className="news-card">
            <img
              src={a.urlToImage || "https://placehold.co/400x200?text=No+Image"}
              alt="News"
              className="news-image"
            />

            <h3 className="news-headline">{a.title}</h3>

            <p className="news-description">
              {a.description || "No description available."}
            </p>

            <a
              href={a.url}
              target="_blank"
              rel="noopener noreferrer"
              className="news-link"
            >
              Read Full Article →
            </a>

            <p className="news-source">
              {a.source?.name} —{" "}
              {new Date(a.publishedAt).toLocaleDateString()}
            </p>
          </div>
        ))}
      </div>
    </div>
  );
}
