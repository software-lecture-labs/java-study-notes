<a id="top"></a>

# Bölüm 13: HTTP Boundary, DTO, Validasyon ve Hata Sözleşmeleri

<!-- nav:top:start -->
[← Bölüm 12](java-chapter-12.md#top) · [İçindekiler](README.md#top)
<!-- nav:top:end -->

Artık elimizde kullanım senaryolarını *(user stories)* koordine eden bir servis tasarımı, olaylarla haberleşen gözlemciler *(Observers)* ve verileri kalıcı olarak saklayan bir katman *(repository)* var. Eksik olan tek şey dış dünya. Şu ana kadar sistemi yalnızca main metodunun olduğu sınıf ve birim testler üzerinden çalıştırdık. Pek tabii ki gerçek hayat senaryolarında sistemin dış dünya tarafından da erişilebilir olması gerekir.

İşte bu bölümde `sales` modülüne bir HTTP katmanı ekleyeceğiz. Ancak Payara ve JAX-RS'e geçmeden önce bilinçli olarak JDK ile birlikte gelen basit HTTP sunucusunu *(`com.sun.net.httpserver`)* kullanacağız. Daha önceki bölümlerde **Dependency Injection Container**'ın, **Java Persistence API**'nın neyi otomatikleştirdiğini görmek için ne yaptıysak burada da aynı strateji ile devam edeceğiz. Bir JAX-RS runtime'ının bizim yerimize yaptığı işleri bir kez elle yazacağız ve aşağıdaki soruların cevabını bulacağız.

- HTTP katmanı domain nesnelerini bilmeli midir? Kullanım senaryolarına *(User Stories)* nasıl erişmelidir?
- Girdi doğrulama *(Validation)* nerede yapılmalıdır? Sınırda mı, domain'de mi, yoksa her ikisinde birden mi?
- Bir domain exception'ı hangi HTTP durum koduna karşılık gelir? Bu kararı nasıl vermeliyiz?
- Bir API servisinden dönecek hatalar istemciye nasıl anlatmalıdır? *(RFC 9457 - Problem Details mevzusu)*
