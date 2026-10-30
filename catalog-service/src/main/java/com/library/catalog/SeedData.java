package com.library.catalog;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class SeedData {

    @Bean
    ApplicationRunner seedCatalog(AuthorRepository authors, PublisherRepository publishers, BookRepository books) {
        return args -> {
            if (books.count() > 0) return;

            Publisher yky = publishers.save(publisher("Yapı Kredi Yayınları", "İstanbul"));
            Publisher iletisim = publishers.save(publisher("İletişim Yayınları", "İstanbul"));
            Publisher can = publishers.save(publisher("Can Yayınları", "İstanbul"));

            Author pamuk = authors.save(author("Orhan Pamuk", 1952, null, "Türkiye",
                    "Nobel Edebiyat Ödülü sahibi (2006) Türk romancı. Eserlerinde İstanbul, kimlik ve "
                            + "Doğu-Batı ilişkisini işler."));
            Author tanpinar = authors.save(author("Ahmet Hamdi Tanpınar", 1901, 1962, "Türkiye",
                    "Şair, romancı ve edebiyat tarihçisi. Zaman ve hafıza temalarıyla tanınır."));
            Author sait = authors.save(author("Sait Faik Abasıyanık", 1906, 1954, "Türkiye",
                    "Türk öykücülüğünün öncülerinden. Sıradan insanın ve denizin yazarı."));
            Author kafka = authors.save(author("Franz Kafka", 1883, 1924, "Avusturya-Macaristan",
                    "Prag doğumlu, Almanca yazan modernist yazar."));

            books.save(book("Kar", pamuk, iletisim, 436, "Roman", 2002, 3));
            books.save(book("Benim Adım Kırmızı", pamuk, yky, 472, "Roman", 1998, 2));
            books.save(book("İstanbul: Hatıralar ve Şehir", pamuk, yky, 348, "Anı", 2003, 1));
            books.save(book("Saatleri Ayarlama Enstitüsü", tanpinar, yky, 382, "Roman", 1961, 2));
            books.save(book("Beş Şehir", tanpinar, yky, 264, "Deneme", 1946, 1));
            books.save(book("Semaver", sait, yky, 120, "Öykü", 1936, 2));
            books.save(book("Dönüşüm", kafka, can, 80, "Roman", 1915, 4));
            books.save(book("Dava", kafka, can, 232, "Roman", 1925, 2));
        };
    }

    private static Publisher publisher(String name, String city) {
        Publisher p = new Publisher();
        p.setName(name);
        p.setCity(city);
        return p;
    }

    private static Author author(String name, Integer born, Integer died, String nationality, String bio) {
        Author a = new Author();
        a.setName(name);
        a.setBirthYear(born);
        a.setDeathYear(died);
        a.setNationality(nationality);
        a.setBiography(bio);
        return a;
    }

    private static Book book(String title, Author a, Publisher p, int pages, String category, int year, int copies) {
        Book b = new Book();
        b.setTitle(title);
        b.setAuthorId(a.getId());
        b.setPublisherId(p.getId());
        b.setPageCount(pages);
        b.setCategory(category);
        b.setPublishYear(year);
        b.setTotalCopies(copies);
        b.setAvailableCopies(copies);
        return b;
    }
}
