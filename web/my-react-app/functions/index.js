const functions = require("firebase-functions");
const fetch = require("node-fetch");

exports.api = functions.https.onRequest(async (req, res) => {
  // CORS Setup
  res.set("Access-Control-Allow-Origin", "*");
  res.set("Access-Control-Allow-Headers", "Content-Type, Authorization");

  if (req.method === "OPTIONS") return res.status(204).send("");

  try {
    const targetUrl = "https://nms-ai-backend-596631887809.europe-west1.run.app/news/doctor";
    
    const response = await fetch(targetUrl, {
      method: "GET",
      headers: {
        "Authorization": req.headers.authorization || "", 
        "Content-Type": "application/json"
      }
    });

    const contentType = response.headers.get("content-type");
    if (!contentType || !contentType.includes("application/json")) {
      const text = await response.text();
      console.error("Backend sent HTML/Text instead of JSON:", text);
      return res.status(502).json({ error: "Backend sent non-JSON data" });
    }

    const data = await response.json();
    return res.status(response.status).json(data);

  } catch (error) {
    console.error("Cloud Function Error:", error.message);
    return res.status(500).json({ error: "Proxy failed", details: error.message });
  }
});