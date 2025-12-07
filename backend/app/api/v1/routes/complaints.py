from fastapi import APIRouter, Depends, HTTPException
from typing import List
from app.core.auth import get_current_user
from app.api.v1.schemas.complaint_schemas import ComplaintCreate, ComplaintResponse, ComplaintUpdate
from app.services.complaints.complaints_service import ComplaintsService

router = APIRouter()
service = ComplaintsService()

@router.post("/", response_model=ComplaintResponse)
def create_complaint(complaint: ComplaintCreate, current_user: dict = Depends(get_current_user)):
    return service.create_complaint(current_user["uid"], complaint)

@router.get("/me", response_model=List[ComplaintResponse])
def read_my_complaints(current_user: dict = Depends(get_current_user)):
    return service.get_user_complaints(current_user["uid"])

@router.get("/", response_model=List[ComplaintResponse])
def read_all_complaints(current_user: dict = Depends(get_current_user)):
    # In a real app, check for admin role here
    # if not current_user.get("is_admin"): raise HTTPException...
    return service.get_all_complaints()

@router.put("/{complaint_id}", response_model=ComplaintResponse)
def update_complaint(complaint_id: str, update: ComplaintUpdate, current_user: dict = Depends(get_current_user)):
    # Check admin role
    result = service.update_complaint_status(complaint_id, update.status, update.admin_response)
    if not result:
        raise HTTPException(status_code=404, detail="Complaint not found")
    return result
