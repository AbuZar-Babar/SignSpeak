You are a research assistant helping us prove the novelty and academic contribution of our custom-built Pakistan Sign Language (PSL) dataset for a Final Year Project (FYP). Your task is to analyze our dataset, compare it against ALL known existing PSL datasets, and produce a rigorous, well-structured novelty justification that can be presented to a supervisor or evaluation panel.

SECTION A: OUR DATASET
Dataset Name: Dynamic Word-Level Pakistan Sign Language Dataset
Published At: https://www.kaggle.com/datasets/mohib123456/dynamic-word-level-pakistan-sign-language-dataset
License: Apache 2.0

Key Specifications:

Gesture Type: Dynamic (temporal/motion-based) word-level gestures — NOT static images or alphabet fingerspelling
Vocabulary Size: 60+ unique word-level sign classes, selected based on words most commonly used in day-to-day Pakistani conversations (referenced from the official PSL website)
Data Representation: MediaPipe hand landmark coordinates (X, Y, Z) — NOT raw video or images
Samples Per Sign: 70 recordings per word (50 from webcam/fixed cameras + 20 from mobile cameras)
Sequence Length: Fixed at 60 frames per gesture
Multi-Device Recording:
mp_data/ folder: 50 samples per sign from standard webcams and fixed cameras
mp_data_mobile/ folder: 20 samples per sign from mobile phone cameras
This multi-camera approach deliberately reduces overfitting to specific lens types, angles, and lighting conditions
Storage Format: Numerical landmark arrays (not videos), making the dataset extremely lightweight (~157 MB compressed vs. tens of GBs for video datasets)
Privacy Preservation: No actual video recordings of signers are stored — only anonymous skeletal landmark data, enhancing participant privacy and security
Intended Use: Training sequence-based deep learning models (LSTM, GRU, Transformers) for real-time sign language translation
SECTION B: KNOWN EXISTING PSL DATASETS FOR COMPARISON
Compare our dataset against each of the following existing PSL datasets. For any additional PSL datasets you find in your knowledge, include those as well.

1. Pakistan Sign Language Dataset (OpenPose) — by Saad Butt (Kaggle)

Link: https://www.kaggle.com/datasets/saadbutt321/pakistan-sign-language-dataset
37 Urdu alphabet signs (~2,000+ images) + 12 Urdu words (~700+ images)
Uses OpenPose skeletal keypoints in JSON format
Collected from 9 non-expert subjects
Primarily static/image-based, not temporal sequences
Origin: FYP at University of Lahore
Size: ~350 MB
2. WLPSL: Word-Level Pakistani Sign Language Dataset — by Jahanzeb Naeem (Kaggle)

Link: https://www.kaggle.com/datasets/jahanzebnaeem/wlpsl
31 word-level classes, 248 total videos
Raw video format (not landmarks)
2 team members + 10 volunteers
Size: ~350 MB (video-heavy)
Models trained on only 15 of the 31 classes
Academic/computational use only license
3. Pakistan Sign Language Urdu Alphabets (Mendeley Data, 2025)

36 static signs (JPG) + 4 dynamic signs (MP4)
Focused on alphabet fingerspelling, not word-level
Mixed format (images + some videos)
4. PkSLMNM Dataset (Mendeley Data)

Focuses on manual and non-manual gestures (facial expressions + body movements)
Contains videos of 7 basic affective expressions from 100 individuals
Not word-level sign vocabulary — emotion/expression focused
5. PSL20 Dataset (Research-level)

20 double-hand dynamic gestures
50 videos per gesture from 5 signers
Limited to 20 classes only
Raw video format
6. Custom Research Datasets (various papers)

2,220 RGB + Kinect video sequences covering 20 gesture classes (from 20 participants)
Various small-scale custom datasets (10–25 classes) used in individual papers
Typically not publicly available
SECTION C: YOUR TASK
Produce a comprehensive novelty analysis with the following structure:

1. Comparison Table
Create a detailed table comparing our dataset against ALL the datasets listed above across these dimensions:

Feature	Our Dataset	Dataset 2	Dataset 3	...
Gesture Type (Static/Dynamic)				
Level (Alphabet/Word/Sentence)				
Vocabulary Size				
Data Format (Video/Image/Landmark)				
Landmark Extraction Tool				
Samples Per Class				
Sequence Length (frames)				
Multi-Device Recording				
Dataset Size				
Privacy Preservation				
Public Availability				
Suitability for Real-Time Deployment				
2. Novelty Dimensions
For each of the following, write a paragraph explaining how our dataset is novel:

Word-Level Dynamic Gestures at Scale — Most PSL datasets are static (alphabet/fingerspelling) or have very small vocabularies (<40 words). Ours has 60+ dynamic word-level signs.

Landmark-Based Instead of Video-Based — We provide pre-extracted MediaPipe landmark coordinates instead of raw videos, which is a deliberate engineering and research decision for:

Space optimization (~157 MB vs. tens of GBs for comparable video datasets)
Privacy/security (no identifiable video of signers)
Direct model input (no preprocessing pipeline needed for consumers of the dataset)
Multi-Camera Generalization — Deliberate recording from webcams, fixed cameras, AND mobile phones to reduce device-specific bias — no other PSL dataset does this systematically.

Standardized Temporal Format — Fixed 60-frame sequences ensure uniform input for sequence models (LSTM/GRU/Transformer), unlike video datasets with variable-length clips.

Practical Vocabulary Selection — Signs selected based on real-world conversational frequency (referenced from official PSL website), not arbitrary academic selection.

Sample Density — 70 samples per class (across multiple devices) provides robust training data compared to datasets with 8–50 samples per class.

Deployment Readiness — The lightweight landmark format makes this dataset directly usable for edge/mobile deployment without GPU-heavy video processing.

3. Gap Analysis
Identify specific gaps in the existing PSL research landscape that our dataset fills. Reference real dataset names and their limitations.

4. One-Paragraph Novelty Statement
Write a formal, academic-style novelty statement (suitable for a thesis or research paper) summarizing why this dataset is a unique contribution to the Pakistan Sign Language recognition field.

Important: Be thorough, specific, and cite actual dataset names, sizes, and limitations. Do not fabricate datasets that don't exist. If you know of any additional PSL datasets beyond what I've listed, include them in the comparison as well.

