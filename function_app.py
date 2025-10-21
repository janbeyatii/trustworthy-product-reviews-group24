# special entry point for Azure 
import azure.functions as func
from fastapi import FastAPI
from fastapi.responses import FileResponse

#FastAPI
api = FastAPI(title="Trustworthy Product Reviews API")

class Review(BaseModel):
    product_id: int
    stars: int
    text: str | None = None

@api.get("/", include_in_schema=False)
def home():
    return FileResponse("static/index.html")

#Expose FastAPI via Azure Functions (ASGI)
app = func.AsgiFunctionApp(app=api, http_auth_level=func.AuthLevel.ANONYMOUS)