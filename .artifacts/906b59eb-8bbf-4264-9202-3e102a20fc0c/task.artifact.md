# Havamania Kullanıcı Verisi İzolasyonu Görev Listesi

- [ ] **1. Veri Katmanı ve Ayarlar**
    - [ ] `AppThemeManager.kt`: Genel ayarları (Tema, Birim, Dil) UID bazlı hale getir ve varsayılanları temizle.
- [ ] **2. ViewModel İzolasyonu ve Temizlik**
    - [ ] `WeatherViewModel.kt`: Auth listener ekle, state'leri anında temizle ve `NoCity` durumunu yönet.
    - [ ] `TravelViewModel.kt`: Auth listener ekle ve seyahat listesini temizle.
    - [ ] `AiHistoryViewModel.kt`: Auth listener ekle ve geçmişi temizle.
    - [ ] `AiChatScreen.kt` (ViewModel): Auth listener ekle ve mesajları temizle.
- [ ] **3. UI Güncellemeleri**
    - [ ] `WeatherView.kt`: `WeatherUiState.NoCity` durumunda uygun Empty State'i göster.
- [ ] **4. Doğrulama**
    - [ ] Kullanıcı A ve B arasında veri sızıntısı olmadığını doğrula.
    - [ ] Yeni kullanıcı başlangıcının tamamen boş olduğunu kontrol et.
