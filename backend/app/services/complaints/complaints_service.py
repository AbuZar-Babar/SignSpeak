from firebase_admin import firestore
from app.api.v1.schemas.complaint_schemas import ComplaintCreate, ComplaintStatus
from datetime import datetime
import uuid

class ComplaintsService:
    def __init__(self):
        try:
            self.db = firestore.client()
            self.collection = self.db.collection('complaints')
        except Exception as e:
            print(f"Firestore init failed: {e}")
            self.db = None

    def create_complaint(self, user_id: str, complaint: ComplaintCreate):
        if not self.db:
            # Mock for dev
            return {
                "id": str(uuid.uuid4()),
                "user_id": user_id,
                "title": complaint.title,
                "description": complaint.description,
                "status": ComplaintStatus.PENDING,
                "created_at": datetime.utcnow(),
                "admin_response": None
            }
            
        doc_ref = self.collection.document()
        data = {
            "id": doc_ref.id,
            "user_id": user_id,
            "title": complaint.title,
            "description": complaint.description,
            "status": ComplaintStatus.PENDING,
            "created_at": datetime.utcnow(),
            "admin_response": None
        }
        doc_ref.set(data)
        return data

    def get_all_complaints(self):
        if not self.db:
            return []
        docs = self.collection.stream()
        return [doc.to_dict() for doc in docs]

    def get_user_complaints(self, user_id: str):
        if not self.db:
            return []
        docs = self.collection.where("user_id", "==", user_id).stream()
        return [doc.to_dict() for doc in docs]

    def update_complaint_status(self, complaint_id: str, status: str, response: str = None):
        if not self.db:
            return None
        doc_ref = self.collection.document(complaint_id)
        update_data = {"status": status}
        if response:
            update_data["admin_response"] = response
        doc_ref.update(update_data)
        return doc_ref.get().to_dict()
