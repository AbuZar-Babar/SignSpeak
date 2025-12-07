from fastapi import APIRouter, Depends, HTTPException
from app.core.auth import get_current_user
from app.api.v1.schemas.user_schemas import UserResponse, UserUpdate

router = APIRouter()

@router.get("/me", response_model=UserResponse)
def read_users_me(current_user: dict = Depends(get_current_user)):
    """
    Get current user profile from Firebase token.
    """
    return {
        "uid": current_user["uid"],
        "email": current_user.get("email"),
        "full_name": current_user.get("name")
    }

@router.put("/me", response_model=UserResponse)
def update_user_me(user_update: UserUpdate, current_user: dict = Depends(get_current_user)):
    """
    Update user profile. 
    Note: In a real app, this would update Firestore or Firebase Auth profile.
    For now, we just echo back combined data.
    """
    # Logic to update Firestore would go here
    return {
        "uid": current_user["uid"],
        "email": current_user.get("email"),
        "full_name": user_update.full_name or current_user.get("name")
    }
