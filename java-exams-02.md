# Alıştırma 02 *(Tasarım Kalıpları: Senaryolar)*

Bu testin amacı kurumsal projelerde sık kullanılan tasarım kalıplarını *(Strategy, State, Chain of Responsibility, Decorator, Adapter, Facade, Command)* ezbere değil, senaryolar üzerinden değerlendirmektir. Sorular bir kalıbın ne zaman gerçekten değer ürettiğini, kalıp sırasının davranışa etkisini, sorumluluğun nerede durması gerektiğini ve benzer görünen kalıpların farklarını **Java** kod parçalarıyla ölçer. Her soru bağımsızdır. Testin sonunda cevap anahtarı yer almaktadır.

*Not: Sorular Java 21 *(JDK 21)* sürümü üzerinden hazırlanmıştır.*

---

## Soru 1

Bir e-ticaret uygulamasında kargo ücreti iki kurala göre hesaplanıyor: yurt içi gönderiler için sabit **49,90 TL**, yurt dışı gönderiler için sabit **199,90 TL**. İş birimi bu yapının önümüzdeki yıllarda değişmesini beklemediğini açıkça belirtmiş. Mevcut kod şu şekilde:

```java
public double shippingFee(Shipment shipment) {
    return shipment.isDomestic() ? 49.90 : 199.90;
}
```

Ekibe yeni katılan bir geliştirici, kodu **Strategy** kalıbına taşımak için bir `ShippingStrategy` arayüzü, `DomesticShipping` ve `InternationalShipping` sınıfları ile bu stratejileri tutan bir `EnumMap` içeren bir pull request açıyor. Code review sırasında yazılabilecek en isabetli yorum hangisidir?

- A) Değişiklik yerindedir. Kod içinde koşullu ifade *(if / ternary)* kalması her durumda **Open/Closed Principle** ihlalidir ve Strategy kalıbı ile giderilmelidir.
- B) Mevcut kod yeterlidir. Strategy kalıbı aynı iş için birden fazla ve değişmesi beklenen algoritma olduğunda, yeni kuralların sık eklendiği veya kuralların bağımsız test edilmesi gerektiği durumlarda değer üretir. İki dallı ve değişmesi beklenmeyen bir kural için arayüz ve sınıflar eklemek kodu sadeleştirmez, dağıtır.
- C) Strategy yerine **State** kalıbı kullanılmalıdır. Çünkü gönderinin yurt içi ya da yurt dışı olması, nesnenin bir durumudur.
- D) Strategy yerine **Chain of Responsibility** kullanılmalıdır. Böylece yurt içi kuralı uygulanamazsa talep yurt dışı kuralına devredilir.

## Soru 2

Bir yazılım şirketi iki farklı modül üzerinde çalışmaktadır.

- **Faturalandırma modülü:** Kullanıcı ödeme sayfasında *aylık*, *yıllık* veya *kullandıkça öde* planlarından birini seçer. Seçilen plana göre tutar farklı bir algoritma ile hesaplanır. Planlar birbirinden habersizdir ve kullanıcı istediği zaman başka bir plana geçebilir.
- **Doküman onay modülü:** Bir doküman **Taslak -> İncelemede -> Onaylandı / Reddedildi** şeklinde ilerler. Taslaktaki doküman düzenlenebilir ama onaylanamaz, incelemedeki doküman düzenlenemez ama onaylanabilir. Bir işlem tamamlandığında dokümanın hangi duruma geçeceğine dokümanın o anki durumu karar verir.

Bu iki modül için hangi tasarım kalıpları en uygundur?

- A) Her iki modül için de **Strategy**
- B) Her iki modül için de **State**
- C) Faturalandırma için **Strategy**, doküman onayı için **State**
- D) Faturalandırma için **State**, doküman onayı için **Strategy**

## Soru 3

Sipariş yaşam döngüsü **State** kalıbı ile modellenmiştir. Her durum ayrı bir sınıftır ve geçersiz işlemler arayüzdeki varsayılan metotlarla reddedilir.

```java
public interface OrderState {
    String name();
    default void pay(Order order)     { throw invalid("ödenemez"); }
    default void ship(Order order)    { throw invalid("kargoya verilemez"); }
    default void cancel(Order order)  { throw invalid("iptal edilemez"); }

    private IllegalStateException invalid(String action) {
        return new IllegalStateException(name() + " durumundaki sipariş " + action);
    }
}

public class Order {
    private OrderState state = new NewState();

    public void pay()    { state.pay(this); }
    public void ship()   { state.ship(this); }
    public void cancel() { state.cancel(this); }

    void transitionTo(OrderState next) { this.state = next; }
    public String getStatus() { return state.name(); }
}
```

Mevcut durumlar `NewState`, `PaidState`, `ShippedState` ve `CancelledState` sınıflarıdır. *(Bu soru için teslim adımı ve olay geçmişi kod kesitinden çıkarılmıştır.)* Yeni bir iş kuralı gelir: *"Kargoya verilmiş bir sipariş için müşteri iade talebi oluşturabilir. Sipariş bu durumda RETURN_REQUESTED durumuna geçer. Diğer durumlarda iade talebi reddedilir."* Bu değişiklik için aşağıdaki adımlar önerilmiştir.

- **I.** `RETURN_REQUESTED` durumunu temsil eden yeni bir `ReturnRequestedState` sınıfı yazılır.
- **II.** `OrderState` arayüzüne varsayılan olarak exception fırlatan bir `requestReturn(Order order)` metodu, `Order` sınıfına da işi duruma devreden bir `requestReturn()` metodu eklenir.
- **III.** `ShippedState` sınıfı `requestReturn` metodunu ezerek *(override)* siparişi `ReturnRequestedState` durumuna geçirir.
- **IV.** `NewState`, `PaidState` ve `CancelledState` sınıflarının her birine, iade talebini reddetmek için exception fırlatan bir `requestReturn` metodu eklenir.

Bu iş kuralını uygulamak için hangi adımlar gerekli ve yeterlidir?

- A) I ve III
- B) Hepsi
- C) II ve IV
- D) I, II ve III

## Soru 4

Bir bankanın havale/EFT modülünde her transfer talebi **Chain of Responsibility** kalıbıyla kurulmuş bir zincirden geçmektedir. Herhangi bir halka talebi reddederse akış durur. Halkalar ve maliyetleri şöyledir:

| Halka | Ne kontrol eder? | Maliyet |
| --- | --- | --- |
| `RequestFormatCheck` | IBAN biçimi, tutarın pozitif olması | Bellekte, mikro saniyeler |
| `DescriptionRequiredCheck` | 50.000 TL ve üzerindeki transferlerde açıklamanın boş olup olmadığı | Bellekte, mikro saniyeler |
| `BalanceCheck` | Gönderen hesabın bakiyesi | Veritabanı sorgusu |
| `BlacklistCheck` | Alıcının kara listede olup olmadığı | Uzak bir dolandırıcılık servisi, ~300 ms, çağrı başına ücretli |

Mevcut zincir `BlacklistCheck -> BalanceCheck -> RequestFormatCheck -> DescriptionRequiredCheck` sırasıyla kurulmuştur. Loglar, reddedilen taleplerin büyük çoğunluğunun hatalı IBAN veya eksik açıklama nedeniyle reddedildiğini gösteriyor. Halkalar birbirinden bağımsızdır ve iş birimi, bir talep birden fazla kuralı ihlal ettiğinde hangi ret gerekçesinin döndüğünü önemsemediğini belirtmiştir.

Ölçülen ret nedenleri ve belirtilen çağrı maliyetleri dikkate alındığında, aşağıdaki sıralamalardan hangisi gereksiz veritabanı ve uzak servis çağrılarını azaltmak için en uygundur?

- A) `RequestFormatCheck -> DescriptionRequiredCheck -> BalanceCheck -> BlacklistCheck`
- B) `BlacklistCheck -> RequestFormatCheck -> DescriptionRequiredCheck -> BalanceCheck`, çünkü güvenlik kontrolleri her zaman ilk sırada olmalıdır.
- C) Sıra önemli değildir. Zincirdeki tüm halkalar her talep için mutlaka çalışır.
- D) `BalanceCheck -> BlacklistCheck -> RequestFormatCheck -> DescriptionRequiredCheck`, çünkü bakiyesi yetersiz talepler en sık karşılaşılan durumdur.

## Soru 5

Bir bankada transfer kontrolleri **Chain of Responsibility** kalıbıyla kurulmuştur. `TransferService`, yapıcı metot üzerinden zincirin ilk halkasını alır ve hangi kontrollerin çalıştığını bilmez.

```java
public class TransferService {
    private final TransferCheck firstCheck;

    public TransferService(TransferCheck firstCheck) {
        this.firstCheck = firstCheck;
    }

    public TransferResult validate(TransferRequest request) {
        return firstCheck.check(request);
    }
}
```

Yeni gereksinimler şöyledir: **Mobil kanalın** zinciri beş kontrolün tamamını *(biçim, kara liste, bakiye, günlük limit, açıklama)* içermeli ve doğrulama ilk rette sonlanmalıdır. **Şube kanalında** günlük limit uygulanmamalı, diğer kontroller aynen kalmalıdır. **Kurumsal kanalda** ise mobil kanaldaki kontrollere ek olarak ikinci imza kontrolü yapılmalıdır.

Bu gereksinimleri karşılamak için en uygun yaklaşım hangisidir?

- A) `DailyLimitCheck` sınıfının içine `if (request.channel() == Channel.BRANCH) return Optional.empty();` gibi bir kanal kontrolü eklemek.
- B) `TransferService` sınıfına `skipDailyLimit` ve `requireSecondSignature` gibi **boolean** parametreler eklemek ve zinciri bu parametrelere göre servis içinde kurmak.
- C) Aynı halka sınıflarını farklı sıralar ve kombinasyonlarla bağlayan üç ayrı zincir tanımlamak *(örneğin `TransferChecks.mobile()`, `branch()`, `corporate()`)*, kurumsal kanal için yeni bir `SecondSignatureCheck` halkası yazmak ve her kanala ait `TransferService` örneğine ilgili zincirin ilk halkasını vermek. Her zincir kendi halka örneklerini oluşturur.
- D) Her kanal için `TransferService` sınıfından türeyen bir alt sınıf yazıp tüm kontrolleri bu alt sınıfların içinde iç içe **if** blokları ile yeniden kodlamak.

## Soru 6

Döviz kuru getiren bir servise önbellekleme, loglama ve yeniden deneme yetenekleri **Decorator** kalıbı ile eklenmiştir. Üç dekoratör de `ExchangeRateProvider` arayüzünü uygular ve kurucu metotlarında sarmaladıkları nesneyi *(delegate)* alır.

```java
public interface ExchangeRateProvider {
    double getRate(String currencyPair);
}

public class CachingRateProvider implements ExchangeRateProvider {
    private final ExchangeRateProvider delegate;
    private final Map<String, Double> cache = new ConcurrentHashMap<>();

    public CachingRateProvider(ExchangeRateProvider delegate) { this.delegate = delegate; }

    @Override
    public double getRate(String currencyPair) {
        return cache.computeIfAbsent(currencyPair, delegate::getRate);
    }
}

public class LoggingRateProvider implements ExchangeRateProvider {
    private final ExchangeRateProvider delegate;
    private final Consumer<String> log;

    public LoggingRateProvider(ExchangeRateProvider delegate, Consumer<String> log) {
        this.delegate = delegate;
        this.log = log;
    }

    @Override
    public double getRate(String currencyPair) {
        log.accept(currencyPair + " isteniyor");
        try {
            double rate = delegate.getRate(currencyPair);
            log.accept(currencyPair + " = " + rate);
            return rate;
        } catch (RuntimeException e) {
            log.accept(currencyPair + " alınamadı: " + e.getMessage());
            throw e;
        }
    }
}

public class RetryingRateProvider implements ExchangeRateProvider {
    private final ExchangeRateProvider delegate;
    private final int maxAttempts;

    public RetryingRateProvider(ExchangeRateProvider delegate, int maxAttempts) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts en az 1 olmalı");
        }
        this.delegate = delegate;
        this.maxAttempts = maxAttempts;
    }

    @Override
    public double getRate(String currencyPair) {
        IllegalStateException lastError = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return delegate.getRate(currencyPair);
            } catch (IllegalStateException e) {
                lastError = e;
            }
        }
        throw lastError;
    }
}
```

`RemoteRateClient(2)` nesnesi uzak servisi temsil eder. İlk **2** çağrıda `IllegalStateException` *(zaman aşımı)* fırlatır, sonraki çağrılarda kuru döner. `getCallCount()` uzak servise kaç kez gidildiğini verir. `computeIfAbsent` çağrısında hesaplama exception ile biterse önbelleğe kayıt eklenmez. Bir geliştirici dekoratörleri aşağıdaki sırayla birleştiriyor.

```java
RemoteRateClient client = new RemoteRateClient(2);
List<String> log = new ArrayList<>();

ExchangeRateProvider provider =
        new RetryingRateProvider(
                new LoggingRateProvider(
                        new CachingRateProvider(
                                new RemoteExchangeRateProvider(client)),
                        log::add),
                3);

provider.getRate("USD/TRY");
provider.getRate("USD/TRY");

System.out.println(client.getCallCount() + " / " + log.size());
```

Sizce bu kodun ekran çıktısı ne olur?

- A) `3 / 8`
- B) `3 / 4`
- C) `1 / 2`
- D) `5 / 8`

## Soru 7

Bir döviz kuru servisine önbellek yeteneği **Decorator** kalıbıyla eklenmiştir. `CachingRateProvider`, bir kur çiftini ilk kez aldığında değeri uygulama çalıştığı sürece saklamaktadır.

```java
ExchangeRateProvider provider =
        new CachingRateProvider(
                new RetryingRateProvider(
                        new RemoteExchangeRateProvider(client), 3));
```

Uygulama günlerce yeniden başlatılmadan çalıştığı için kullanıcılar sabahki kuru akşam da görmeye başlamıştır. İş birimi, önbellekte **5 dakikasını dolduran** bir kurun bir sonraki sorguda uzak servisten yenilenmesini istiyor. Aynı modülü kullanan **raporlama** ekibi ise her çalıştırmada tek bir kapanış gününe ait sabit kurlarla çalıştığı için mevcut, süresiz önbellekten memnundur. İstemci kodlarının `ExchangeRateProvider` arayüzü dışında hiçbir şey bilmemesi isteniyor.

Bu ihtiyacı karşılamak için en uygun yaklaşım hangisidir?

- A) `CachingRateProvider` sınıfına `boolean expiring` ve `Duration ttl` parametreleri eklemek ve metodun içinde bu bayrağa göre iki farklı önbellek davranışı uygulamak.
- B) Önbelleği tamamen kaldırıp her çağrıda uzak servise gitmek. Böylece kur her zaman güncel olur.
- C) `RemoteExchangeRateProvider` sınıfından türeyen `ExpiringRemoteExchangeRateProvider` ve `CachingRemoteExchangeRateProvider` gibi alt sınıflar yazmak ve her yetenek kombinasyonu için yeni bir alt sınıf oluşturmak.
- D) Aynı arayüzü uygulayan ve süreyi dışarıdan alan *(saat bilgisini de test edilebilirlik için `java.time.Clock` olarak enjekte eden)* yeni bir `ExpiringCachingRateProvider` dekoratörü yazmak. Kur ekranında bu dekoratörü, raporlamada mevcut `CachingRateProvider`'ı kullanmak. Hangisinin kullanılacağına nesneler oluşturulurken karar vermek.

## Soru 8

Bir e-ticaret uygulaması iki sanal POS sağlayıcısı ile **Adapter** kalıbı üzerinden çalışmaktadır. İş kodu yalnızca `PaymentGateway` arayüzünü tanır. Aşağıdaki varsayımsal ilk sürümde tutar `double` olarak alınmakta ve her adapter onu kendi formatına kendisi çevirmektedir.

```java
public interface PaymentGateway {
    PaymentResult charge(String orderNo, double amount);
}

// PayFast SDK'sı tutarı kuruş cinsinden long bekler
public class PayFastAdapter implements PaymentGateway {
    public PaymentResult charge(String orderNo, double amount) {
        long kurus = Math.round(amount * 100);
        // client.makePayment(orderNo, kurus, "TRY") ...
    }
}

// Banka kütüphanesi tutarı virgüllü metin olarak bekler
public class LegacyBankPosAdapter implements PaymentGateway {
    public PaymentResult charge(String orderNo, double amount) {
        String text = String.format(Locale.ROOT, "%.2f", amount).replace('.', ',');
        // pos.process("<amount>" + text + "</amount> ...") ...
    }
}
```

Bir gün **1,005 TL** tutarlı bir test siparişinde PayFast'in **100 kuruş** *(1,00 TL)*, bankanın ise **1,01 TL** çektiği fark ediliyor. *(`1.005` değeri double olarak `1.00499...` şeklinde tutulduğu için `Math.round(1.005 * 100)` ifadesi `100`, `String.format(Locale.ROOT, "%.2f", 1.005)` ifadesi ise `"1.01"` sonucunu üretir.)*

Ödeme sözleşmesine göre tutar pozitif olmalı ve tam kuruş olarak ifade edilebilmelidir. Kuruşun kesrini içeren talepler herhangi bir sağlayıcı çağrılmadan reddedilmelidir. Bu sözleşmeyi tüm sağlayıcılar için tek yerde hangi tasarım uygular?

- A) Her iki adapter'da da aynı yuvarlama yöntemini kullanmak, örneğin bankada da `Math.round(amount * 100)` ile kuruşa çevirip metne dönüştürmek.
- B) Tutarı sistem sınırında *(örneğin `CheckoutService` içinde)* bir kez doğrulayıp kuruş cinsinden tutan bir değer nesnesine *(`Amount` gibi bir value object)* çevirmek, kuruştan küçük hassasiyetli tutarları yuvarlamak yerine reddetmek ve `PaymentGateway` arayüzünün bu değer nesnesini almasını sağlamak. Adapter'lar artık yalnızca format dönüşümü yapar.
- C) `double` yerine `float` kullanmak. Daha az basamak tutulduğu için yuvarlama farkları ortadan kalkar.
- D) Adapter kalıbını kaldırıp iki sağlayıcının çağrılarını `CheckoutService` içinde `if-else` ile yapmak. Böylece tüm dönüşümler tek bir sınıfta toplanır.

## Soru 9

Bir şirketin iki farklı ekibi iki farklı entegrasyon problemiyle uğraşmaktadır.

- **Fatura ekibi:** Fatura PDF'i üretmek için üçüncü parti bir PDF kütüphanesi kullanıyor. Basit bir faturayı oluşturmak için bile kütüphanenin `Document`, `PageLayout`, `FontRegistry`, `TableBuilder`, `Renderer` gibi on iki farklı sınıfının doğru sırayla kullanılması gerekiyor. Ekip, uygulamanın geri kalanının sadece `invoicePdf.render(invoice)` gibi tek bir metot görmesini istiyor.
- **Ödeme ekibi:** Uygulama zaten `PaymentGateway` adında kendi arayüzünü tanımlamış ve tüm iş kodu bu arayüzle çalışıyor. Yeni anlaşılan bir sağlayıcının SDK'sı tutarı farklı birimde, sonucu farklı hata kodlarıyla döndürüyor. Ekip, bu SDK'nın mevcut `PaymentGateway` arayüzüne uymasını istiyor.

Bu iki ihtiyaç için hangi kalıplar en uygundur?

- A) Fatura ekibi için **Facade**, ödeme ekibi için **Adapter**
- B) Fatura ekibi için **Adapter**, ödeme ekibi için **Facade**
- C) Her iki ekip için de **Adapter**
- D) Fatura ekibi için **Decorator**, ödeme ekibi için **Adapter**

## Soru 10

Sıra tabanlı bir strateji oyununda hamlelerin geri alınabilmesi için **Command** kalıbı kullanılmıştır. Oyun tahtası bir birimin can puanını asla sıfırın altına düşürmez. *(`Command` arayüzünün `execute` ve `undo` dışındaki üyeleri kesitte gösterilmemiştir.)*

```java
public class GameBoard {
    private final Map<String, Integer> healthPoints = new HashMap<>();

    public int hpOf(String unit) { return healthPoints.get(unit); }

    public void setHp(String unit, int hp) {
        healthPoints.put(unit, Math.max(0, hp));
    }
    // diğer metotlar...
}

public class AttackCommand implements Command {
    private final GameBoard board;
    private final String target;
    private final int damage;

    public AttackCommand(GameBoard board, String target, int damage) {
        this.board = board;
        this.target = target;
        this.damage = damage;
    }

    @Override
    public void execute() {
        board.setHp(target, board.hpOf(target) - damage);
    }

    @Override
    public void undo() {
        board.setHp(target, board.hpOf(target) + damage);
    }
}
```

Canı **20** olan bir birime **35** hasarlık bir saldırı yapılıyor *(`execute`)*, ardından oyuncu bu hamleyi geri alıyor *(`undo`)*. Geri alma işleminden sonra birimin canı kaç olur ve doğru tasarım ne olmalıdır?

- A) Can **20** olur. Mevcut tasarım doğrudur, değişiklik gerekmez.
- B) Can **35** olur. Hedefin kalan canı sıfırın altına düşemediği için "hasar kadar geri ekle" yaklaşımı yanlış sonuç verir. Komut, `execute` sırasında hedefin önceki can değerini saklamalı ve `undo` bu değeri geri yüklemelidir.
- C) Can **0** olur. Çünkü `undo` metodu ölü bir birim üzerinde çalışmaz.
- D) Can **-15** olur. Bu durumu düzeltmek için `GameBoard` sınıfındaki `Math.max(0, hp)` kontrolü kaldırılmalıdır.

---

## Cevap Anahtarı

| Soru No | Doğru Cevap |
| ------- | ----------- |
| 1 | B |
| 2 | C |
| 3 | D |
| 4 | A |
| 5 | C |
| 6 | A |
| 7 | D |
| 8 | B |
| 9 | A |
| 10 | B |
