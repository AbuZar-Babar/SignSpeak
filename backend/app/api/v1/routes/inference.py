from fastapi import APIRouter, UploadFile, File, HTTPException, Depends
from app.services.mediapipe.mediapipe_service import MediaPipeService
from app.services.inference.inference_service import InferenceService
from app.api.v1.schemas.inference_schemas import PredictionResponse
from app.core.auth import get_current_user

router = APIRouter()
mediapipe_service = MediaPipeService()
inference_service = InferenceService()

@router.post("/predict", response_model=PredictionResponse)
async def predict(file: UploadFile = File(...)):
    # TODO: Re-enable auth after mobile app auth is implemented
    # current_user: dict = Depends(get_current_user)
    current_user = {"uid": "test_user"} 
    try:
        contents = await file.read()
        landmarks = mediapipe_service.process_frame(contents)
        
        if landmarks is None:
            raise HTTPException(status_code=400, detail="Could not extract landmarks")
            
        # Use user_id to maintain sequence state per user
        user_id = current_user["uid"]
        result = inference_service.predict(user_id, landmarks)
        
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
