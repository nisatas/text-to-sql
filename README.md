#  Doğal Dil (NLP) ile Öğrenci Veri Sorgulama Sistemi (Text-to-SQL)

Bu proje, öğretmenlerin ve okul yöneticilerinin teknik bilgiye (SQL) ihtiyaç duymadan, sadece doğal dil kullanarak öğrenci veritabanı üzerinde sorgulama yapabilmelerini sağlayan bir **Text-to-SQL** sistemidir.

##  Proje Özeti
Mevcut durumda öğrenci verileri genellikle Excel dosyalarında tutulmakta, bu verilere ulaşmak ve analiz yapmak zaman alıcı ve karmaşık olmaktadır. Bu sistem, kullanıcının yazdığı doğal dil ifadelerini otomatik olarak SQL sorgusuna çevirerek veritabanından sonuçları getirir ve kullanıcıya sunar.

**Örnek Kullanım:** *"Matematik notu 50'nin altında olan öğrenciler"* 

---

##  Teknik Mimari (Tech Stack)

### **Frontend (Ön Yüz)**
* **Framework:** Angular 
* **UI Bileşenleri:** Angular Material 
* **İletişim:** Backend ile REST API üzerinden veri alışverişi 

### **Backend (Sunucu Katmanı)**
* **Dil & Framework:** Java & Spring Boot 
* **Yapay Zeka (AI):** Llama 3 (Ollama) — Yerel Dil Modeli (LLM)
* **AI Entegrasyonu** Spring AI — Backend ile Llama 3 arasındaki iletişimi sağlayan köprü.
*  **Mekanizma:** Doğal dilden PostgreSQL uyumlu sorgu üreten Text-to-SQL motoru.

### **Veritabanı (Database)**
* **Sistem:** PostgreSQL (İlişkisel Veritabanı) 
* **Tablo Yapıları:** `students`, `grades` ve `classes` tabloları arasında kurulan ilişkisel (Foreign Key) mimari.

---
