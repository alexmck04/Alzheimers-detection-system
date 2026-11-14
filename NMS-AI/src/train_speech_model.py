import os
import numpy as np
import librosa
import tensorflow as tf

def extract_features(file_path):
    y, sr = librosa.load(file_path, sr=16000) #sampling rate
    mfcc = librosa.feature.mfcc(y=y, sr=sr, n_mfcc=40) #higher-order coefficients
    return np.mean(mfcc.T, axis=0)

def load_data(base_dir):
    x, y = [], []
    for label, category in enumerate(['cc', 'cd']):
        folder = os.path.join(base_dir, 'Normalised_audio-chunks', category)
        for file in os.listdir(folder):
            if file.endswith('.wav'):
                try: 
                    features = extract_features (os.path.join(folder, file))
                    x.append(features)
                    y.append(label)
                except Exception as e:
                    print(f"Error processing {file}: {e}")
    return np.array(x), np.array(y)

if __name__ == "__main__":
    x, y = load_data("../datasets/train")
    model = tf.keras.Sequential([
        tf.keras.layers.Dense(128, activation='relu', input_shape=(40,)),
        tf.keras.layers.Dropout(0.3),
        tf.keras.layers.Dense(64, activation='relu'),
        tf.keras.layers.Dense(1, activation='sigmoid')
    ])
    model.compile(optimizer='adam', loss='binary_crossentropy', metrics=['accuracy'])
    model.fit(x,y, epochs=25, batch_size=32, validation_split=0.2)
    model.save("../models/speech_model.h5")
    print("Speech model saved!")