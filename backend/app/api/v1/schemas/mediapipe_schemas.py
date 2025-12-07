from pydantic import BaseModel
from typing import List, Optional

class Landmark(BaseModel):
    x: float
    y: float
    z: float

class HandLandmarks(BaseModel):
    landmarks: List[Landmark]

class MediaPipeResponse(BaseModel):
    hands: List[List[Landmark]]
