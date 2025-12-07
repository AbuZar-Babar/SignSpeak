from pydantic import BaseModel
from typing import Optional
from enum import Enum
from datetime import datetime

class ComplaintStatus(str, Enum):
    PENDING = "Pending"
    IN_PROGRESS = "In Progress"
    RESOLVED = "Resolved"
    REJECTED = "Rejected"

class ComplaintBase(BaseModel):
    title: str
    description: str

class ComplaintCreate(ComplaintBase):
    pass

class ComplaintUpdate(BaseModel):
    status: ComplaintStatus
    admin_response: Optional[str] = None

class ComplaintResponse(ComplaintBase):
    id: str
    user_id: str
    status: ComplaintStatus
    created_at: datetime
    admin_response: Optional[str] = None

    class Config:
        from_attributes = True
