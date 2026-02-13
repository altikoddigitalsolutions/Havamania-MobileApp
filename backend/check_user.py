import sys
import os

# Backend modüllerini içe aktarabilmek için yolu ekle
sys.path.append(os.getcwd())

from app.db.session import SessionLocal
from app.models.user import User

def check_user():
    db = SessionLocal()
    email = "test@example.com"
    
    try:
        user = db.query(User).filter(User.email == email).first()
        if user:
            print(f"Kullanıcı BULUNDU: {user.email}")
            print(f"Full Name: {user.full_name}")
        else:
            print(f"Kullanıcı BULUNAMADI: {email}")
    except Exception as e:
        print(f"Hata: {e}")
    finally:
        db.close()

if __name__ == "__main__":
    check_user()
