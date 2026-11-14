import numpy as np
import tensorflow as tf
import librosa

model_path = "/app/models/speech_model.h5"
_model = None

def get_model():
    global _model
    if _model is None:
        _model = tf.keras.models.load_model(model_path)
    return _model

def extract_features(file_path):
    y, sr = librosa.load(file_path, sr=16000)
    mfcc = librosa.feature.mfcc(y=y, sr=sr, n_mfcc=40)
    return np.mean(mfcc.T, axis=0)

def predict_from_audio(file_path):
    model = get_model()
    features = extract_features(file_path)
    pred = model.predict(np.expand_dims(features, axis=0))[0][0]
    return float(pred)