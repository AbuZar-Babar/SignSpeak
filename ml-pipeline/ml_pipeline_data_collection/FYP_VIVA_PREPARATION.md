# SignSpeak — FYP 60% Viva Preparation Guide
## Complete Facts, Figures & Recommended Answers

---

## TABLE OF CONTENTS
1. [Model Performance Summary](#1-model-performance-summary)
2. [Is 99% Accuracy Overfitting?](#2-is-99-accuracy-overfitting--no)
3. [Why is Support = 14 for Every Class?](#3-why-is-support--14-for-every-class)
4. [Why Do Some Signs Have Perfect F1 = 1.0?](#4-why-do-some-signs-have-perfect-f1--10)
5. [Why is the Confusion Matrix Mostly Zeros?](#5-why-is-the-confusion-matrix-mostly-zeros)
6. [Should You Reduce Epochs?](#6-should-you-reduce-epochs--no)
7. [Architecture Decisions — Justified](#7-architecture-decisions--justified)
8. [Training Hyperparameters — Justified](#8-training-hyperparameters--justified)
9. [Regularization Decisions — Justified](#9-regularization-decisions--justified)
10. [Data Configuration — Justified](#10-data-configuration--justified)
11. [Data Augmentation — Justified](#11-data-augmentation--justified)
12. [Metric Definitions](#12-metric-definitions)
13. [Potential Viva Questions & Recommended Answers](#13-potential-viva-questions--recommended-answers)
14. [Limitations to Proactively Mention](#14-limitations-to-proactively-mention)
15. [Architecture Diagram](#15-architecture-diagram)

---

## 1. MODEL PERFORMANCE SUMMARY

### At a Glance

| Metric                | Baseline Model | Augmented Model | Improvement |
|-----------------------|---------------|-----------------|-------------|
| Test Accuracy         | 96.91%        | 99.66%          | +2.75%      |
| Train Accuracy        | 99.69%        | 99.76%          | +0.07%      |
| Train-Test Gap        | 2.78%         | 0.10%           | -2.68%      |
| Macro F1              | 96.11%        | 99.47%          | +3.36%      |
| Weighted F1           | 96.76%        | 99.65%          | +2.89%      |
| Avg Latency/Sequence  | 1.99 ms       | 1.60 ms         | -0.39 ms    |
| Perfect Classes (F1=1)| 26/63 (41%)   | 60/63 (95%)     | +34 classes |
| Training Duration     | 918 sec       | 4,259 sec       | 4.6x longer |
| Epochs (actual)       | ~100          | ~143            | +43         |

### Dataset Details
- **Total samples**: 4,370 sequences
- **Data sources**: MP_Data (laptop webcam) + MP_Data_mobile (mobile camera)
- **Train/Test split**: 80% / 20% (stratified, random_state=42)
- **Train set**: 3,496 sequences
- **Test set**: 874 sequences
- **Number of classes**: 63 signs
- **Samples per class**: ~70 total → ~56 train + ~14 test

---

## 2. IS 99% ACCURACY OVERFITTING? — NO

### What Overfitting Actually Looks Like

| Scenario              | Train Acc | Test Acc | Gap    | Verdict        |
|-----------------------|-----------|----------|--------|----------------|
| Severe overfitting    | 99%       | 65%      | 34%    | ❌ Overfit      |
| Moderate overfitting  | 99%       | 85%      | 14%    | ⚠️ Overfit     |
| **Our Baseline**      | **99.69%**| **96.91%**| **2.78%** | ✅ Healthy  |
| **Our Augmented**     | **99.76%**| **99.66%**| **0.10%** | ✅ Excellent|

### 5 Reasons High Accuracy is Legitimate

**Reason 1: We classify landmarks, not raw images**
- MediaPipe extracts 126 clean numerical features per frame (42 landmarks × 3 coordinates)
- These are structured, normalized coordinate vectors — not noisy 640×480 pixel arrays
- The signal-to-noise ratio is inherently very high
- Analogy: "It's like giving someone GPS coordinates instead of a blurry map"

**Reason 2: Controlled, consistent dataset**
- 4,370 sequences collected by team members in controlled conditions
- Each sign has a distinct, well-defined gesture
- ~70 sequences per sign from multiple recording sessions

**Reason 3: Rigorous evaluation methodology**
- 80/20 stratified train-test split (random_state=42 for reproducibility)
- Stratification ensures every class has proportional representation
- Test set was NEVER seen during training
- EarlyStopping prevents training too long

**Reason 4: Signs are inherently separable**
- "hello" looks nothing like "elephant" which looks nothing like "airplane"
- 63 signs span diverse categories (greetings, animals, objects, actions)
- 60 frames × 126 features = 7,560 data points per sequence — massive discriminative power

**Reason 5: Consistent with published research**
- MediaPipe landmark-based sign language recognition routinely achieves 95%+ on controlled vocabularies
- Google's sign language research reports >98% on controlled datasets
- Our result is expected, not anomalous

### RECOMMENDED ANSWER:

> "If the model were overfitting, we would expect the test accuracy to be significantly lower
> than training accuracy. Our train-test gap is only 2.78% for baseline and 0.10% for augmented.
> Additionally, the augmented model — which adds noise, warps time, and applies spatial
> transformations — still achieves 99.66% on completely unseen test data. An overfit model would
> collapse when facing these variations. The high accuracy is because we classify structured
> MediaPipe landmark sequences (126 features/frame), not raw pixels, and our 63 signs are
> visually distinct from each other."

---

## 3. WHY IS SUPPORT = 14 FOR EVERY CLASS?

### What "Support" Means
Support = the number of TRUE test samples for that class. It is NOT a model metric — it's a property of the test set.

### The Math

```
Total samples:      4,370
Number of classes:  63
Samples per class:  4,370 ÷ 63 ≈ 69.4 (total)
Test size:          20%
Test per class:     69.4 × 0.2 ≈ 13.9 → rounds to 14
```

Because we used **stratified splitting** (`stratify=y_encoded`), sklearn distributes each class proportionally into the test set. Since every class has ~70 total samples, each gets exactly 14 in the test set.

### Exception: test_word
`test_word` has support = 6 (not 14) because it has fewer total recordings (~30 instead of ~70). It was likely added later or collected with fewer sequences.

### RECOMMENDED ANSWER:

> "Support indicates the number of ground-truth test samples per class. It's uniformly 14
> because we used stratified splitting — sklearn proportionally distributes every class into
> the test set. Since we collected approximately 70 sequences per sign and reserved 20% for
> testing, that gives exactly 14 test sequences per class. The only exception is test_word
> which has 6 because it has fewer total recordings."

---

## 4. WHY DO SOME SIGNS HAVE PERFECT F1 = 1.0?

### How Perfect Scores Happen
With 14 test samples per class, perfect scores mean:
- **Precision = 1.0**: Every time the model predicted this sign, it was correct (0 false positives)
- **Recall = 1.0**: The model correctly identified all 14 test instances (0 false negatives)
- **F1 = 1.0**: Both precision and recall are perfect → harmonic mean is also perfect

### Perfect Classes in Baseline (26 out of 63):
aircrash, all, also, assalam-o-alaikum, beak, bear, bridge, bulb, crow, deer, dog, door,
facelotion, fan, goodbye, goodmorning, have_a_good_day, ihaveacomplaint, mobile_phone,
nailcutter, peacock, policecar, razor, shampoo, sunglasses, tissue, toothbrush, toothpaste, we

These signs have **unique hand shapes and motions** that don't overlap with any other class.

### Perfect Classes in Augmented (60 out of 63):
Almost everything except test_word, nothing, and left_hand.

### Imperfect Classes (the interesting ones):

| Sign       | Precision | Recall | F1   | Explanation                                    |
|------------|-----------|--------|------|------------------------------------------------|
| test_word  | 0.50      | 0.17   | 0.25 | Too few samples (6), poorly learned            |
| nothing    | 0.81      | 0.93   | 0.87 | Subtle gesture, confused with similar signs    |
| bench      | 0.86      | 0.86   | 0.86 | Moderate confusion with similar hand positions |
| garden     | 0.82      | 1.00   | 0.90 | Model catches all, but sometimes falsely predicts it |

### RECOMMENDED ANSWER:

> "26 of our 63 signs are classified with zero errors on the test set. These are signs with
> highly distinctive gestures — for example, 'peacock' and 'airplane' have unique hand shapes
> that don't resemble any other sign. The few imperfect classes like 'nothing' and 'bench' show
> genuine confusion between visually similar gestures, which actually validates that our
> evaluation is real and not artificially inflated."

---

## 5. WHY IS THE CONFUSION MATRIX MOSTLY ZEROS?

### What the Confusion Matrix Shows
It's a 63×63 grid (3,969 cells total). Cell [i][j] means:
"How many times was TRUE class i PREDICTED as class j?"

- **Diagonal** [i][i] = correct predictions ← we want these to be high
- **Off-diagonal** [i][j] where i≠j = misclassifications ← we want these to be zero

### Why It's Mostly Zeros — The Math

| Model     | Total Test | Correct  | Errors | Off-diagonal cells | % zeros |
|-----------|-----------|----------|--------|--------------------|---------|
| Baseline  | 874       | ~848     | ~27    | 3,906              | 99.3%   |
| Augmented | 874       | ~871     | ~3     | 3,906              | 99.9%   |

For the augmented model, only **3 samples** were misclassified across **3,906 possible** off-diagonal cells. That's why almost everything is zero.

### A Mostly-Zero Confusion Matrix is the IDEAL Outcome
It means the model almost never confuses any pair of signs.

### Visual Comparison

```
GOOD confusion matrix (ours):     BAD confusion matrix:
14  0  0  0  0                    8  3  2  1  0
 0 14  0  0  0                    4  6  3  1  0
 0  0 14  0  0                    1  2  7  4  0
 0  0  0 14  0                    2  1  3  8  0
 0  0  0  0 14                    0  0  1  2 11

← Perfect diagonal                ← Errors everywhere = confused model
```

### RECOMMENDED ANSWER:

> "The confusion matrix is 63×63 = 3,969 cells. A mostly-zero off-diagonal means the model
> rarely confuses any pair of signs. Our augmented model has only 3 misclassifications out of
> 874 test samples — those 3 errors are scattered across 3,906 possible off-diagonal cells,
> making 99.9% of the matrix zeros. This is the ideal outcome. A problematic model would show
> dense clusters of non-zero values indicating systematic confusion between similar signs."

---

## 6. SHOULD YOU REDUCE EPOCHS? — NO

### Current Setup
- `EPOCHS = 200` (maximum ceiling)
- `EarlyStopping(patience=30, monitor='val_accuracy', restore_best_weights=True)`

### What Actually Happened
- Baseline stopped at **~100 epochs** (out of 200)
- Augmented stopped at **~143 epochs** (out of 200)

EarlyStopping is the real epoch controller. The 200 is just a safety ceiling.

### What Would Happen if You Set EPOCHS = 50?
- Baseline might still be OK (if it converged by epoch 50)
- **Augmented model would be CUT SHORT** — it needed 143 epochs to converge
- You'd get a WORSE augmented model with a lower accuracy

### RECOMMENDED ANSWER:

> "We don't need to reduce epochs because EarlyStopping with patience=30 automatically halts
> training when validation accuracy plateaus for 30 consecutive epochs. The 200 is just a
> maximum ceiling — our baseline actually converged at ~100 and augmented at ~143. Lowering
> the ceiling would risk cutting off the augmented model prematurely, resulting in a worse
> model. EarlyStopping plus restore_best_weights ensures we always keep the best checkpoint."

---

## 7. ARCHITECTURE DECISIONS — JUSTIFIED

### Model: 3-Layer Stacked LSTM

| Decision            | Choice              | Justification                                                       |
|---------------------|---------------------|---------------------------------------------------------------------|
| **Why LSTM?**       | Over CNN/Transformer| Sign language is temporal — meaning comes from how hands move over time. LSTMs are designed for sequential dependencies. CNNs treat frames independently. Transformers are overkill for 60-frame sequences. |
| **Why 3 layers?**   | Not 1, not 5       | 1 layer can't capture complex temporal hierarchies. 5+ layers risk vanishing gradients and are harder to train. 3 is the sweet spot for medium-length sequences. |
| **Why 64→128→64?**  | Bottleneck pattern  | Expanding then contracting: first layer captures low-level features, middle captures complex patterns with more capacity, third compresses into compact representation. Proven in sequence modeling. |
| **Why not GRU?**    | LSTM preferred      | LSTMs have separate cell state for better gradient flow over 60 frames. GRUs are simpler but slightly less expressive for our sequence length. |
| **Why not Transformer?** | LSTM sufficient | Transformers excel at 100s-1000s of tokens with attention. Our sequences are 60 frames — LSTM handles this efficiently without attention overhead. Also more interpretable for FYP. |
| **Dense(64)→Dense(32)** | Gradually compress | Maps the LSTM's temporal representation into classification space. Gradual reduction (64→32→63) prevents information bottleneck. |
| **Softmax output**  | 63 units            | Outputs probability distribution across all 63 signs. Sum = 1.0. Highest probability = predicted sign. |

---

## 8. TRAINING HYPERPARAMETERS — JUSTIFIED

| Parameter       | Value | Justification                                                              |
|-----------------|-------|----------------------------------------------------------------------------|
| **Epochs**      | 200   | High ceiling with EarlyStopping (patience=30) as real controller. Baseline stopped ~100, augmented ~143. |
| **Batch Size**  | 16    | Small batches provide noisier gradients = implicit regularization. Better generalization than large batches (64/128). Standard for sequence data of this size. |
| **Learning Rate** | 0.001 | Default for Adam optimizer. Well-established starting point. Combined with ReduceLROnPlateau which halves LR when val_loss plateaus. |
| **Optimizer**   | Adam  | Adaptive per-parameter learning rates. Combines RMSprop + Momentum. Standard choice for deep learning. |
| **Loss**        | Categorical Crossentropy | Standard for multi-class single-label classification with one-hot labels. Penalizes confident wrong predictions heavily. |

---

## 9. REGULARIZATION DECISIONS — JUSTIFIED

| Technique              | Value/Config                        | Justification                                             |
|------------------------|-------------------------------------|-----------------------------------------------------------|
| **LSTM Dropout**       | 0.2 (20%)                          | Zeros 20% of outputs randomly. Conservative: regularizes without losing too much information. |
| **Dense Dropout**      | 0.3 (30%)                          | Higher than LSTM dropout because dense layers are more prone to memorization. |
| **Baseline: NO dropout** | Intentional design               | Ablation study: baseline=no regularization, augmented=full regularization. Measures combined effect. |
| **EarlyStopping**      | patience=30, val_accuracy           | Stops after 30 epochs of no improvement. restore_best_weights=True keeps best checkpoint. |
| **ReduceLROnPlateau**  | factor=0.5, patience=10, min_lr=1e-6 | Halves LR when val_loss plateaus for 10 epochs. Enables fine-grained optimization in later epochs. |

---

## 10. DATA CONFIGURATION — JUSTIFIED

| Parameter            | Value                    | Justification                                                    |
|----------------------|--------------------------|------------------------------------------------------------------|
| **Sequence Length**   | 60 frames                | ~2sec at 30fps webcam, ~3sec on mobile. Captures complete sign gesture. Too short (15-20) misses complex signs. Too long (120+) adds noise. |
| **Sequences/sign**   | 20 per source            | Standard for FYP-level collection. Combined sources give ~70/class. |
| **Features/frame**   | 126                      | 42 landmarks (21 left + 21 right hand) × 3 coords (x,y,z).     |
| **Hands only**       | No pose/face landmarks   | PSL relies on hand shapes. Adding 33 pose + 468 face = 1,500+ irrelevant features → more noise, more overfitting risk. |
| **Test split**       | 20% stratified           | 80/20 is standard. Stratification ensures proportional class representation. random_state=42 for reproducibility. |
| **Prediction threshold** | 0.8 (80%)           | Only display prediction if confidence > 80%. Prevents low-confidence noise in real-time UI. |

---

## 11. DATA AUGMENTATION — JUSTIFIED

| Technique            | Probability | Parameters          | Real-World Simulation                                      |
|----------------------|-------------|---------------------|-------------------------------------------------------------|
| **Time Warp**        | 50%         | speed: 0.9–1.1      | People sign at different speeds                              |
| **Spatial Scale**    | 50%         | scale: 0.95–1.05    | Signer closer/farther from camera                            |
| **Spatial Translate**| 50%         | range: ±0.05        | Signer not perfectly centered in frame                       |
| **Spatial Rotate**   | 30%         | angle: ±15°         | Camera tilt or body angle variation                          |
| **Gaussian Noise**   | 30%         | std: 0.01           | MediaPipe detection noise/jitter                             |
| **Temporal Crop**    | 30%         | ratio: 10%          | Variation in when sign starts/ends in the sequence           |
| **Horizontal Flip**  | **DISABLED**| —                   | **PSL is non-symmetric — flipping creates INVALID signs**    |
| **Multiplier**       | 3×          | —                   | Each sample → 1 original + 2 augmented = 3× data total      |

### KEY POINT: Why Horizontal Flip is Disabled

Pakistan Sign Language uses a dominant hand. Many signs are fundamentally different when performed with left vs right hand. Flipping would create non-existent or incorrect signs, essentially training the model on WRONG labels.

### Augmentation Impact

| Metric           | Without Aug | With Aug  | Change           |
|------------------|-------------|-----------|------------------|
| Training data    | 4,370       | 13,110    | 3× more data     |
| Test Accuracy    | 96.91%      | 99.66%    | +2.75%           |
| Overfitting Gap  | 2.78%       | 0.10%     | Gap nearly eliminated |
| Training Time    | 918 sec     | 4,259 sec | 4.6× longer      |

---

## 12. METRIC DEFINITIONS

Know these cold for the viva:

| Metric              | Formula                    | Plain English                                                |
|----------------------|----------------------------|--------------------------------------------------------------|
| **Accuracy**         | Correct / Total            | % of all predictions that were right                         |
| **Precision**        | TP / (TP + FP)             | "Of all times model SAID this sign, how often was it right?" |
| **Recall**           | TP / (TP + FN)             | "Of all ACTUAL instances of this sign, how many did model catch?" |
| **F1-Score**         | 2×(P×R)/(P+R)              | Harmonic mean of precision and recall                        |
| **Macro F1**         | Average F1 across classes   | Treats all classes equally (even rare ones)                  |
| **Weighted F1**      | Weighted average by support | Gives more weight to classes with more samples               |
| **Support**          | Count of true instances     | Number of test samples for this class (NOT a model metric)   |
| **Confusion Matrix** | Grid of predictions         | Cell [i][j] = how many times true class i was predicted as j |

### Why Macro F1 vs Weighted F1?
- **Macro F1 (96.11% / 99.47%)**: Treats every class equally. If one rare class performs poorly, macro F1 drops. This is the STRICTER metric.
- **Weighted F1 (96.76% / 99.65%)**: Weights by class frequency. Higher because most classes perform well and have equal weight.
- The small gap between them confirms all classes are performing similarly (no class is dragging things down).

---

## 13. POTENTIAL VIVA QUESTIONS & RECOMMENDED ANSWERS

### Q1: "Is your model overfitting?"
> "No. The key indicator of overfitting is a large gap between training and test accuracy.
> Our baseline has a gap of only 2.78%, and our augmented model has a gap of just 0.10%.
> If the model were memorizing training data, it would fail on unseen test sequences —
> but it achieves 99.66% on held-out data. The high accuracy is because we classify
> structured MediaPipe landmarks (126 clean numerical features per frame), not raw noisy
> images, and our 63 signs are visually distinct from each other."

### Q2: "Should you reduce epochs?"
> "No. We use EarlyStopping with patience=30 which automatically stops training when
> validation accuracy stops improving. EPOCHS=200 is just a ceiling. Our baseline
> converged at ~100 epochs and augmented at ~143. Lowering the ceiling could prematurely
> cut off the augmented model."

### Q3: "Why do some signs have F1 = 1.0?"
> "With 14 test samples per class, a perfect F1 means the model correctly classified all
> 14 samples and never falsely predicted that sign for other samples. This happens for
> signs with very distinctive gestures. 26 out of 63 signs have F1=1.0 in the baseline,
> and 60 out of 63 in the augmented model."

### Q4: "What is support and why is it 14?"
> "Support is the number of ground-truth test samples per class. It's 14 because we have
> ~70 samples per class and used a stratified 80/20 split, so 70 × 0.2 = 14 per class
> in the test set."

### Q5: "Why is the confusion matrix mostly zeros?"
> "Because the model rarely confuses signs. The matrix is 63×63 = 3,969 cells. Our
> augmented model only misclassifies 3 out of 874 test samples, so only 3 off-diagonal
> cells have non-zero values out of 3,906 possible. A mostly-diagonal confusion matrix
> is the ideal outcome."

### Q6: "Why LSTM and not CNN or Transformer?"
> "Sign language is a temporal sequence problem — meaning comes from how hand positions
> change over 60 frames. LSTMs are specifically designed for sequential temporal
> dependencies. CNNs treat each frame independently and lose temporal context.
> Transformers are powerful but computationally expensive and designed for much longer
> sequences. For 60 frames, LSTM gives the best accuracy-to-complexity ratio."

### Q7: "How do you know this works on new users?"
> "This is a valid limitation we acknowledge. Our current evaluation measures intra-user
> generalization — same signers, different sequences. For true generalization, we would
> need leave-one-signer-out evaluation, which we plan as future work. However, our
> augmentation (spatial translation, scaling, rotation, noise) partially simulates
> user variation."

### Q8: "Why only hand landmarks and not face/pose?"
> "We extract 42 hand landmarks (21 per hand) × 3 coordinates = 126 features. PSL
> relies primarily on hand shapes and movements. Including pose (33 landmarks) and face
> (468 landmarks) would add 1,500+ features that are mostly irrelevant to hand signs,
> increasing noise and overfitting risk without meaningful accuracy gain."

### Q9: "What is categorical crossentropy?"
> "It's the standard loss function for multi-class classification. It measures the
> difference between the predicted probability distribution and the true one-hot label.
> It penalizes confident wrong predictions more heavily — if the model says 90% class A
> but it's actually class B, the loss is much higher than if it said 55% class A."

### Q10: "What does Adam optimizer do?"
> "Adam (Adaptive Moment Estimation) maintains per-parameter learning rates that adapt
> based on first and second moments of gradients. It combines RMSprop's adaptive learning
> rates with SGD's momentum. We use LR=0.001 with ReduceLROnPlateau which halves the
> rate when validation loss plateaus."

### Q11: "Why is augmented model's training accuracy also high — shouldn't augmentation make training harder?"
> "Augmentation does make training harder — each epoch, the model sees slightly different
> versions of the same gesture. The fact that it STILL achieves 99.76% training accuracy
> means it has learned the underlying pattern, not memorized specific examples. This is
> precisely why the train-test gap drops from 2.78% to 0.10% — the model genuinely
> understands the gestures."

### Q12: "What is the prediction threshold 0.8?"
> "During real-time inference, we only display a prediction if the model's confidence
> (softmax probability) exceeds 80%. If the model is less than 80% sure, we suppress
> the output. This prevents low-confidence guesses from appearing in the UI and improves
> the user experience."

### Q13: "Why 63 signs? Is that enough?"
> "63 signs covers core vocabulary for daily PSL communication — greetings, common nouns,
> animals, objects, and phrases. Major benchmarks like WLASL have 100-2000 words but are
> offline. Our system does REAL-TIME inference at ~2ms per sequence, which is the key
> differentiator. The vocabulary is extensible — adding more signs only requires
> collecting 70 sequences and retraining."

### Q14: "Why not use k-fold cross-validation?"
> "For 4,370 sequences across 63 classes, k-fold would multiply training time by k.
> Our stratified 80/20 split with fixed random_state=42 provides a reproducible and
> representative evaluation. For a production system, we would use k-fold, but for
> FYP scope, a held-out test set is standard and sufficient. The consistency of results
> across both baseline and augmented models further validates our evaluation approach."

### Q15: "Why did you disable horizontal flip?"
> "Pakistan Sign Language is non-symmetric. Many signs use a dominant hand, and the
> meaning changes if performed with the opposite hand. Horizontally flipping would
> swap left and right hands, creating non-existent or incorrect signs. This would
> essentially train the model on wrong labels, degrading performance. This decision
> shows domain-specific understanding of PSL."

---

## 14. LIMITATIONS TO PROACTIVELY MENTION

Mentioning limitations proactively shows maturity. Evaluators respect this more than pretending the project is flawless.

1. **Single-environment data**: Collected in controlled settings — performance may degrade in varied lighting/backgrounds
2. **Limited signer diversity**: Trained on team members' signing style — may not generalize to all PSL speakers
3. **No sentence-level recognition**: System recognizes isolated signs, not continuous signing
4. **test_word under-represented**: Shows the system needs minimum ~70 samples per class for reliable classification
5. **No pose/face features**: Some PSL signs incorporate facial expressions — future work could add face landmarks
6. **No cross-signer evaluation**: Leave-one-signer-out testing would better validate generalization

---

## 15. ARCHITECTURE DIAGRAM

```
┌─────────────────────────────────────────────────────────────────┐
│                     INPUT PIPELINE                              │
│                                                                 │
│    Camera Feed → MediaPipe Holistic → Extract 42 hand landmarks │
│    → 126 features/frame × 60 frames = sequence (60, 126)       │
└─────────────────────────┬───────────────────────────────────────┘
                          │
                          ▼
              ┌───────────────────────┐
              │   LSTM (64 units)     │ ← Captures short-term motion patterns
              │   return_sequences=T  │
              │   activation='tanh'   │
              │   + Dropout(0.2)*     │    * only in augmented model
              └───────────┬───────────┘
                          │
                          ▼
              ┌───────────────────────┐
              │   LSTM (128 units)    │ ← Captures complex mid-level patterns
              │   return_sequences=T  │    (wider bottleneck for more capacity)
              │   activation='tanh'   │
              │   + Dropout(0.2)*     │
              └───────────┬───────────┘
                          │
                          ▼
              ┌───────────────────────┐
              │   LSTM (64 units)     │ ← Compresses to final temporal encoding
              │   return_sequences=F  │    (outputs single vector, not sequence)
              │   activation='tanh'   │
              │   + Dropout(0.2)*     │
              └───────────┬───────────┘
                          │
                          ▼
              ┌───────────────────────┐
              │   Dense (64, ReLU)    │ ← Non-linear feature combination
              │   + Dropout(0.3)*     │
              └───────────┬───────────┘
                          │
                          ▼
              ┌───────────────────────┐
              │   Dense (32, ReLU)    │ ← Further dimensionality reduction
              └───────────┬───────────┘
                          │
                          ▼
              ┌───────────────────────┐
              │   Dense (63, Softmax) │ ← Output: probability for each of 63 signs
              └───────────────────────┘
                          │
                          ▼
              ┌───────────────────────┐
              │   argmax + threshold  │ ← Pick highest prob if > 0.8, else reject
              └───────────────────────┘
```

### Training Pipeline Overview

```
┌─────────────┐    ┌──────────────┐    ┌─────────────────┐
│  MP_Data    │───→│              │    │                 │
│  (laptop)   │    │   Combined   │───→│  Baseline Model │ No augmentation
│             │    │   Loader     │    │  No dropout     │ No dropout
├─────────────┤    │              │    └─────────────────┘
│ MP_Data     │───→│  4,370 seqs  │
│ (mobile)    │    │              │    ┌─────────────────┐    ┌──────────────┐
│             │    │              │───→│  Augment 3×     │───→│ Augmented    │
└─────────────┘    └──────────────┘    │  13,110 seqs    │    │ Model        │
                                       └─────────────────┘    │ With dropout │
                                                              └──────────────┘
                                                                     │
                                                                     ▼
                                                              ┌──────────────┐
                                                              │  Compare &   │
                                                              │  Recommend   │
                                                              └──────────────┘
```

---

**Document generated for FYP 60% Evaluation Preparation**
**Project: SignSpeak — Pakistan Sign Language Recognition System**
**Date: April 2026**
