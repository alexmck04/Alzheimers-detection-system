import os, re, glob
import numpy as np
import tensorflow as tf
import librosa

TEST_DIR = "../datasets/test"
CHUNK_DIR = os.path.join(TEST_DIR, "Normalised_audio-chunks")  # <- correct path
MODEL_PATH = "../models/speech_model.h5"
OUT_PER_FILE = os.path.join(TEST_DIR, "per_file_scores.tsv")
OUT_PER_SUBJ = os.path.join(TEST_DIR, "test_results-classif-1.txt")  # submission format
THRESHOLD = 0.5

def extract_features(file_path):
    y, sr = librosa.load(file_path, sr=16000)
    mfcc = librosa.feature.mfcc(y=y, sr=sr, n_mfcc=40)
    return np.mean(mfcc.T, axis=0)

def predict_file(model, wav_path):
    feat = extract_features(wav_path)
    p = model.predict(np.expand_dims(feat, axis=0), verbose=0)[0][0]
    return float(p)

if __name__ == "__main__":
    assert os.path.isdir(CHUNK_DIR), f"Missing folder: {CHUNK_DIR}"
    assert os.path.exists(MODEL_PATH), f"Missing model: {MODEL_PATH}"
    model = tf.keras.models.load_model(MODEL_PATH)

    wavs = sorted(glob.glob(os.path.join(CHUNK_DIR, "**", "*.wav"), recursive=True))
    print(f"Found {len(wavs)} wav files under {CHUNK_DIR}")

    per_file = []
    for fp in wavs:
        base = os.path.basename(fp)
        m = re.match(r"(S\d+)", base)
        sid = m.group(1) if m else "UNKNOWN"
        prob = predict_file(model, fp)
        per_file.append((fp, sid, prob))

    with open(OUT_PER_FILE, "w", encoding="utf-8") as f:
        f.write("file\tID\tprob\n")
        for fp, sid, p in per_file:
            f.write(f"{fp}\t{sid}\t{p:.6f}\n")
    print(f"Per-file scores written to {OUT_PER_FILE}")

    by_sid = {}
    for _, sid, p in per_file:
        if sid == "UNKNOWN":
            continue
        by_sid.setdefault(sid, []).append(p)

    rows = []
    for sid, probs in sorted(by_sid.items(), key=lambda kv: int(kv[0][1:])):
        avg_prob = float(np.mean(probs))
        pred = 1 if avg_prob >= THRESHOLD else 0  # 1 = AD (cd), 0 = non-AD (cc)
        rows.append((sid, pred))

    with open(OUT_PER_SUBJ, "w", encoding="utf-8") as f:
        f.write("ID\tPrediction\n")
        for sid, pred in rows:
            f.write(f"{sid}\t{pred}\n")
    print(f"Subject-level predictions written to {OUT_PER_SUBJ}")
