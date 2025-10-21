# special entry point for Azure 
import azure.functions as func
from fastapi import FastAPI
from pydantic import BaseModel

#FastAPI
api = FastAPI(title="Trustworthy Product Reviews API")

class Review(BaseModel):
    product_id: int
    stars: int
    text: str | None = None

@api.get("/health")
async def health():
    return {"status": "ok"}

@api.post("/reviews")
async def create_review(r: Review):
    # TODO: call src modules here
    return {"ok": True, "received": r.model_dump()}

#Expose FastAPI via Azure Functions (ASGI)
app = func.AsgiFunctionApp(
    app=api,
    http_auth_level=func.AuthLevel.ANONYMOUS  # Anonymous access means APIs are public
)
