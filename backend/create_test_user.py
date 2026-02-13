import sys
import os
import bcrypt

# Backend modüllerini içe aktarabilmek için yolu ekle
sys.path.append(os.getcwd())

from app.db.session import SessionLocal
from app.models.user import User

def create_test_user():
    db = SessionLocal()
    email = "test@example.com"
    password = "password123"
    full_name = "Test User"
    
    try:
        existing = db.query(User).filter(User.email == email).first()
        if existing:
            print(f"Kullanıcı zaten mevcut: {email}")
            return

        # Passlib hatasını atlamak için doğrudan bcrypt kullanıyoruz
        # password_hash = hash_password(password) kısmını manuel yapıyoruz
        hashed = bcrypt.hashpw(password.encode('utf-8'), bcrypt.gensalt()).decode('utf-8')
        
        user = User(email=email, password_hash=hashed, full_name=full_name)
        db.add(user)
        db.commit()
        db.refresh(user)
        
        print("Test kullanıcısı başarıyla oluşturuldu! (bcrypt bypass kullanıldı)")
        print(f"Email: {user.email}")
        print(f"Password: {password}")
    except Exception as e:
        print(f"Hata oluştu: {e}")
        db.rollback()
    finally:
        db.close()

if __name__ == "__main__":
    create_test_user()
