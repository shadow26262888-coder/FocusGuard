# FocusGuard — Kurulum ve Derleme Talimatı

## 1. Firebase Kurulumu

1. [Firebase Console](https://console.firebase.google.com) → Yeni Proje oluştur
2. Android uygulaması ekle → paket adı: `com.focusguard`
3. İndirilen `google-services.json` dosyasını `app/` klasörüne koy
4. Firebase Realtime Database → Test modunda başlat

## 2. Android Studio'da Açma

```
File → Open → /workspace/FocusGuard klasörünü seç
```

Gradle sync otomatik çalışır.

## 3. İzinler (İlk Kurulumda)

Çocuk cihazında şunları MUTLAKA ver:
- **Cihaz Yöneticisi:** Ayarlar → Güvenlik → Cihaz Yöneticisi → FocusGuard ✓
- **Erişilebilirlik:** Ayarlar → Erişilebilirlik → FocusGuard Koruma → Aç
- **Diğer Uygulamaların Üstünde Göster:** Ayarlar → Uygulamalar → FocusGuard → Özel Erişim → izin ver

## 4. Kullanım

### Çocuk Cihazı:
1. Uygulamayı aç → **Çocuk Modu** seç
2. QR kodu veya 6 haneli kodu ebeveyne göster
3. İzinleri ver (yukarıdaki 3 izin)

### Ebeveyn Cihazı:
1. Uygulamayı aç → **Ebeveyn Modu** seç
2. QR tara veya kodu manuel gir
3. Master kodunu belirle ve sakla
4. **Kilitle / Kilidi Aç** butonlarıyla kontrol et

## 5. Kilit Zamanlaması

- Her gün **18:00'dan itibaren** otomatik kilitlenir
- **08:00'dan sonra** kilit otomatik **açılmaz** (ebeveyn açar)
- Manuel kilitleme/açma ebeveyn panelinden

## 6. Acil Çıkış

Ses tuşuna **2 saniye içinde 6 kez** hızlıca bas.
→ Tüm kısıtlamalar kalkar, uygulama devre dışı olur.
→ Ebeveyne bildirim gitmez.

## 7. Güvenlik Notları

- Master kodu hiçbir zaman çocuğa gösterme
- Firebase Database kurallarını production'da güçlendir:
```json
{
  "rules": {
    "devices": {
      "$deviceId": {
        ".read": "auth != null",
        ".write": "auth != null"
      }
    }
  }
}
```
