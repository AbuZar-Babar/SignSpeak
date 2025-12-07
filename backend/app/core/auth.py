import firebase_admin
from firebase_admin import auth, credentials
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
import os

# Initialize Firebase Admin
# We try to initialize it. If no credentials are found, it might fail in a real env 
# without GOOGLE_APPLICATION_CREDENTIALS set.
# For development without keys, we might need a mock or just let it fail if called.
try:
    if not firebase_admin._apps:
        firebase_admin.initialize_app()
except Exception as e:
    print(f"Warning: Firebase Admin initialization failed: {e}")

security = HTTPBearer()

def get_current_user(creds: HTTPAuthorizationCredentials = Depends(security)):
    """
    Validates the Firebase ID token.
    """
    token = creds.credentials
    try:
        # verify_id_token will raise an error if the token is invalid
        decoded_token = auth.verify_id_token(token)
        return decoded_token
    except Exception as e:
        # For development/testing purposes, if we don't have a valid firebase setup,
        # we might want to allow a bypass if a specific header is present, 
        # BUT for production/SRS compliance we must enforce it.
        # For now, strictly enforce.
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail=f"Invalid authentication credentials: {str(e)}",
            headers={"WWW-Authenticate": "Bearer"},
        )
