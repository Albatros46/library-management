"""Adds 100 English- and German-language authors, one well-known book each, through the gateway API.

Safe to run more than once: authors, books and publishers that already exist (same name/title) are skipped.
Page counts are typical edition lengths; editions differ.

    python e2e/sample_data.py                      # http://localhost:8080
    BASE_URL=http://localhost:3000 python e2e/sample_data.py
"""
import json
import os
import random
import urllib.error
import urllib.request

BASE = os.environ.get("BASE_URL", "http://localhost:8080").rstrip("/")
ADMIN = {"email": os.environ.get("ADMIN_EMAIL", "admin@library.local"),
         "password": os.environ.get("ADMIN_PASSWORD", "admin123")}

PUBLISHERS = {
    "Penguin Classics": "London", "Penguin Books": "London", "Vintage": "London", "HarperCollins": "London",
    "Bloomsbury": "London", "Faber & Faber": "London", "Fourth Estate": "London", "Pan Books": "London",
    "Scribner": "New York", "Ace Books": "New York",
    "Reclam": "Ditzingen", "Suhrkamp": "Berlin", "S. Fischer": "Frankfurt am Main", "Diogenes": "Zürich",
    "Rowohlt": "Hamburg", "Kiepenheuer & Witsch": "Köln", "Steidl": "Göttingen", "Hanser": "München",
    "dtv": "München", "Thienemann": "Stuttgart", "Droemer": "München", "Dressler": "Hamburg",
}

# (author, born, died, nationality, biography, title, year, pages, category, publisher)
DATA = [
    # --- English ---
    ("George Orwell", 1903, 1950, "İngiltere", "Totaliter rejimleri ve propaganda dilini eleştiren İngiliz yazar ve gazeteci.",
     "Nineteen Eighty-Four", 1949, 328, "Distopya", "Penguin Books"),
    ("Aldous Huxley", 1894, 1963, "İngiltere", "Bilim, toplum ve özgürlük üzerine yazan İngiliz romancı ve denemeci.",
     "Brave New World", 1932, 311, "Distopya", "Vintage"),
    ("Jane Austen", 1775, 1817, "İngiltere", "İngiliz taşra soylularının evlilik ve sınıf ilişkilerini ironik bir dille anlatan romancı.",
     "Pride and Prejudice", 1813, 432, "Klasik", "Penguin Classics"),
    ("Charles Dickens", 1812, 1870, "İngiltere", "Viktorya dönemi Londra'sının yoksulluğunu ve toplumsal adaletsizliğini anlatan romancı.",
     "Great Expectations", 1861, 544, "Klasik", "Penguin Classics"),
    ("Charlotte Brontë", 1816, 1855, "İngiltere", "Bağımsız kadın kahramanlarıyla tanınan Brontë kardeşlerin en büyüğü.",
     "Jane Eyre", 1847, 532, "Klasik", "Penguin Classics"),
    ("Emily Brontë", 1818, 1848, "İngiltere", "Tek romanı ile İngiliz edebiyatının en tutkulu aşk hikâyelerinden birini yazan şair ve romancı.",
     "Wuthering Heights", 1847, 416, "Klasik", "Penguin Classics"),
    ("Mary Shelley", 1797, 1851, "İngiltere", "Bilim kurgunun öncülerinden sayılan İngiliz romancı.",
     "Frankenstein", 1818, 280, "Klasik", "Penguin Classics"),
    ("Oscar Wilde", 1854, 1900, "İrlanda", "Keskin nükteleri, oyunları ve estetizmi ile tanınan İrlandalı yazar.",
     "The Picture of Dorian Gray", 1890, 254, "Klasik", "Penguin Classics"),
    ("Virginia Woolf", 1882, 1941, "İngiltere", "Bilinç akışı tekniğinin öncülerinden, modernist İngiliz yazar.",
     "Mrs Dalloway", 1925, 194, "Roman", "Vintage"),
    ("James Joyce", 1882, 1941, "İrlanda", "Modernist romanın en etkili isimlerinden İrlandalı yazar.",
     "Dubliners", 1914, 224, "Öykü", "Penguin Classics"),
    ("Thomas Hardy", 1840, 1928, "İngiltere", "Kırsal İngiltere'de kader ve toplum baskısını anlatan romancı ve şair.",
     "Tess of the d'Urbervilles", 1891, 518, "Klasik", "Penguin Classics"),
    ("H. G. Wells", 1866, 1946, "İngiltere", "Zaman yolculuğu ve uzaylı istilası gibi temaları ilk işleyen bilim kurgu yazarı.",
     "The Time Machine", 1895, 118, "Bilim Kurgu", "Penguin Classics"),
    ("Arthur Conan Doyle", 1859, 1930, "İskoçya", "Sherlock Holmes karakterinin yaratıcısı İskoç yazar ve hekim.",
     "The Hound of the Baskervilles", 1902, 256, "Polisiye", "Penguin Classics"),
    ("Agatha Christie", 1890, 1976, "İngiltere", "Hercule Poirot ve Miss Marple'ın yaratıcısı, 'polisiyenin kraliçesi'.",
     "Murder on the Orient Express", 1934, 256, "Polisiye", "HarperCollins"),
    ("J. R. R. Tolkien", 1892, 1973, "İngiltere", "Orta Dünya'yı yaratan filolog ve modern fantastik edebiyatın kurucusu.",
     "The Hobbit", 1937, 310, "Fantastik", "HarperCollins"),
    ("C. S. Lewis", 1898, 1963, "İngiltere", "Narnia Günlükleri'nin yazarı, edebiyat akademisyeni ve denemeci.",
     "The Lion, the Witch and the Wardrobe", 1950, 208, "Çocuk", "HarperCollins"),
    ("Lewis Carroll", 1832, 1898, "İngiltere", "Matematikçi ve yazar; absürt ve oyunbaz çocuk edebiyatının ustası.",
     "Alice's Adventures in Wonderland", 1865, 192, "Çocuk", "Penguin Classics"),
    ("Robert Louis Stevenson", 1850, 1894, "İskoçya", "Macera romanları ve Dr. Jekyll ile tanınan İskoç yazar.",
     "Treasure Island", 1883, 292, "Macera", "Penguin Classics"),
    ("Rudyard Kipling", 1865, 1936, "İngiltere", "Hindistan doğumlu, Nobel ödüllü İngiliz yazar ve şair.",
     "The Jungle Book", 1894, 277, "Çocuk", "Penguin Classics"),
    ("Joseph Conrad", 1857, 1924, "İngiltere", "Polonya doğumlu, sömürgecilik ve deniz üzerine yazan İngiliz romancı.",
     "Heart of Darkness", 1899, 96, "Klasik", "Penguin Classics"),
    ("William Golding", 1911, 1993, "İngiltere", "İnsan doğasının karanlık yanını işleyen Nobel ödüllü romancı.",
     "Lord of the Flies", 1954, 224, "Roman", "Faber & Faber"),
    ("Kazuo Ishiguro", 1954, None, "İngiltere", "Japonya doğumlu, hafıza ve pişmanlık temalarıyla tanınan Nobel ödüllü İngiliz yazar.",
     "The Remains of the Day", 1989, 258, "Roman", "Faber & Faber"),
    ("Ian McEwan", 1948, None, "İngiltere", "Psikolojik gerilimli romanlarıyla tanınan çağdaş İngiliz yazar.",
     "Atonement", 2001, 371, "Roman", "Vintage"),
    ("Zadie Smith", 1975, None, "İngiltere", "Çok kültürlü Londra'yı anlatan çağdaş İngiliz romancı.",
     "White Teeth", 2000, 448, "Roman", "Penguin Books"),
    ("J. K. Rowling", 1965, None, "İngiltere", "Harry Potter serisinin yazarı İngiliz romancı.",
     "Harry Potter and the Philosopher's Stone", 1997, 223, "Fantastik", "Bloomsbury"),
    ("Terry Pratchett", 1948, 2015, "İngiltere", "Diskdünya serisiyle fantastik edebiyata mizah katan İngiliz yazar.",
     "The Colour of Magic", 1983, 288, "Fantastik", "HarperCollins"),
    ("Douglas Adams", 1952, 2001, "İngiltere", "Mizahi bilim kurgunun en sevilen isimlerinden İngiliz yazar.",
     "The Hitchhiker's Guide to the Galaxy", 1979, 224, "Bilim Kurgu", "Pan Books"),
    ("Arthur C. Clarke", 1917, 2008, "İngiltere", "Bilimsel öngörüleriyle ünlü İngiliz bilim kurgu yazarı.",
     "2001: A Space Odyssey", 1968, 297, "Bilim Kurgu", "HarperCollins"),
    ("Neil Gaiman", 1960, None, "İngiltere", "Mitolojiyi modern dünyayla buluşturan İngiliz fantastik yazar.",
     "American Gods", 2001, 635, "Fantastik", "HarperCollins"),
    ("Hilary Mantel", 1952, 2022, "İngiltere", "Thomas Cromwell üçlemesiyle iki kez Booker kazanan İngiliz romancı.",
     "Wolf Hall", 2009, 653, "Tarihi Roman", "Fourth Estate"),
    ("Mark Twain", 1835, 1910, "ABD", "Mississippi kıyılarını mizahla anlatan Amerikan edebiyatının babası.",
     "Adventures of Huckleberry Finn", 1884, 366, "Klasik", "Penguin Classics"),
    ("Herman Melville", 1819, 1891, "ABD", "Denizcilik deneyimlerinden yola çıkan Amerikalı romancı.",
     "Moby-Dick", 1851, 635, "Klasik", "Penguin Classics"),
    ("F. Scott Fitzgerald", 1896, 1940, "ABD", "Caz Çağı'nın ve Amerikan rüyasının kronikçisi.",
     "The Great Gatsby", 1925, 180, "Roman", "Scribner"),
    ("Ernest Hemingway", 1899, 1961, "ABD", "Yalın üslubuyla tanınan Nobel ödüllü Amerikalı yazar.",
     "The Old Man and the Sea", 1952, 127, "Roman", "Scribner"),
    ("John Steinbeck", 1902, 1968, "ABD", "Büyük Buhran dönemi işçilerini anlatan Nobel ödüllü romancı.",
     "The Grapes of Wrath", 1939, 464, "Roman", "Penguin Books"),
    ("William Faulkner", 1897, 1962, "ABD", "Amerikan Güneyi'ni deneysel anlatımla işleyen Nobel ödüllü yazar.",
     "The Sound and the Fury", 1929, 326, "Roman", "Vintage"),
    ("Harper Lee", 1926, 2016, "ABD", "Irkçılık ve adalet üzerine yazdığı tek romanıyla Pulitzer kazanan yazar.",
     "To Kill a Mockingbird", 1960, 281, "Roman", "Vintage"),
    ("J. D. Salinger", 1919, 2010, "ABD", "Gençlik yabancılaşmasının simgesi olan romanın inzivaya çekilmiş yazarı.",
     "The Catcher in the Rye", 1951, 234, "Roman", "Penguin Books"),
    ("Ray Bradbury", 1920, 2012, "ABD", "Şiirsel bilim kurgu ve fantastik öyküleriyle tanınan Amerikalı yazar.",
     "Fahrenheit 451", 1953, 256, "Distopya", "HarperCollins"),
    ("Isaac Asimov", 1920, 1992, "ABD", "Robot yasaları ve Vakıf serisiyle tanınan biyokimyacı ve bilim kurgu yazarı.",
     "Foundation", 1951, 255, "Bilim Kurgu", "HarperCollins"),
    ("Frank Herbert", 1920, 1986, "ABD", "Ekoloji ve siyaseti harmanlayan Dune serisinin yazarı.",
     "Dune", 1965, 617, "Bilim Kurgu", "Ace Books"),
    ("Ursula K. Le Guin", 1929, 2018, "ABD", "Antropolojik bilim kurgu ve fantastik eserleriyle tanınan Amerikalı yazar.",
     "A Wizard of Earthsea", 1968, 183, "Fantastik", "Penguin Books"),
    ("Philip K. Dick", 1928, 1982, "ABD", "Gerçeklik ve kimlik sorgulayan bilim kurgu romanlarının yazarı.",
     "Do Androids Dream of Electric Sheep?", 1968, 210, "Bilim Kurgu", "Penguin Books"),
    ("Toni Morrison", 1931, 2019, "ABD", "Afroamerikan deneyimini anlatan Nobel ödüllü romancı.",
     "Beloved", 1987, 324, "Roman", "Vintage"),
    ("Cormac McCarthy", 1933, 2023, "ABD", "Şiddet ve hayatta kalma üzerine sert, yalın romanlarıyla tanınan yazar.",
     "The Road", 2006, 287, "Roman", "Vintage"),
    ("Stephen King", 1947, None, "ABD", "Korku ve gerilim türünün en çok okunan Amerikalı yazarı.",
     "The Shining", 1977, 447, "Korku", "Penguin Books"),
    ("Edgar Allan Poe", 1809, 1849, "ABD", "Korku öyküsü ve polisiyenin öncüsü Amerikalı şair ve yazar.",
     "The Raven and Other Poems", 1845, 91, "Şiir", "Penguin Classics"),
    ("Kurt Vonnegut", 1922, 2007, "ABD", "Savaşı kara mizahla anlatan Amerikalı romancı.",
     "Slaughterhouse-Five", 1969, 275, "Roman", "Vintage"),
    ("Sylvia Plath", 1932, 1963, "ABD", "İtirafçı şiirin öncüsü Amerikalı şair ve romancı.",
     "The Bell Jar", 1963, 244, "Roman", "Faber & Faber"),
    ("Margaret Atwood", 1939, None, "Kanada", "Distopik ve feminist romanlarıyla tanınan Kanadalı yazar.",
     "The Handmaid's Tale", 1985, 311, "Distopya", "Vintage"),
    # --- German ---
    ("Johann Wolfgang von Goethe", 1749, 1832, "Almanya", "Alman edebiyatının en büyük şairi; Faust'un yazarı, Weimar klasizminin kurucusu.",
     "Die Leiden des jungen Werthers", 1774, 160, "Klasik", "Reclam"),
    ("Friedrich Schiller", 1759, 1805, "Almanya", "Özgürlük temalı dramlarıyla tanınan şair ve oyun yazarı.",
     "Die Räuber", 1781, 176, "Tiyatro", "Reclam"),
    ("Heinrich Heine", 1797, 1856, "Almanya", "Romantizmin son, ironik ve siyasi şairi.",
     "Buch der Lieder", 1827, 256, "Şiir", "Reclam"),
    ("Jacob Grimm", 1785, 1863, "Almanya", "Kardeşi Wilhelm ile Alman masallarını derleyen dilbilimci.",
     "Kinder- und Hausmärchen", 1812, 500, "Çocuk", "Reclam"),
    ("E. T. A. Hoffmann", 1776, 1822, "Almanya", "Fantastik ve tekinsiz öyküleriyle tanınan romantik yazar, besteci ve hukukçu.",
     "Der Sandmann", 1816, 64, "Öykü", "Reclam"),
    ("Theodor Fontane", 1819, 1898, "Almanya", "Alman gerçekçiliğinin önde gelen romancısı.",
     "Effi Briest", 1895, 352, "Klasik", "Reclam"),
    ("Gottfried Keller", 1819, 1890, "İsviçre", "İsviçre gerçekçiliğinin en önemli anlatıcısı.",
     "Kleider machen Leute", 1874, 80, "Öykü", "Reclam"),
    ("Georg Büchner", 1813, 1837, "Almanya", "Genç yaşta ölen devrimci oyun yazarı ve hekim.",
     "Woyzeck", 1879, 96, "Tiyatro", "Reclam"),
    ("Friedrich Nietzsche", 1844, 1900, "Almanya", "Ahlak ve din eleştirisiyle modern düşünceyi etkileyen filozof.",
     "Also sprach Zarathustra", 1883, 352, "Felsefe", "Reclam"),
    ("Thomas Mann", 1875, 1955, "Almanya", "Burjuva ailelerin çöküşünü anlatan Nobel ödüllü romancı.",
     "Buddenbrooks", 1901, 768, "Roman", "S. Fischer"),
    ("Heinrich Mann", 1871, 1950, "Almanya", "Wilhelm dönemi toplumunu hiciv ile eleştiren romancı; Thomas Mann'ın ağabeyi.",
     "Der Untertan", 1918, 496, "Roman", "S. Fischer"),
    ("Hermann Hesse", 1877, 1962, "Almanya", "Bireyin iç yolculuğunu anlatan Nobel ödüllü yazar.",
     "Siddhartha", 1922, 128, "Roman", "Suhrkamp"),
    ("Stefan Zweig", 1881, 1942, "Avusturya", "Novella ve biyografileriyle dünya çapında okunan Avusturyalı yazar.",
     "Schachnovelle", 1942, 112, "Öykü", "S. Fischer"),
    ("Rainer Maria Rilke", 1875, 1926, "Avusturya", "Prag doğumlu, Almanca şiirin en önemli modern şairlerinden.",
     "Briefe an einen jungen Dichter", 1929, 96, "Deneme", "Reclam"),
    ("Robert Musil", 1880, 1942, "Avusturya", "Niteliksiz Adam'ın yazarı, deneysel modernist romancı.",
     "Die Verwirrungen des Zöglings Törleß", 1906, 224, "Roman", "Rowohlt"),
    ("Joseph Roth", 1894, 1939, "Avusturya", "Habsburg İmparatorluğu'nun çöküşünü anlatan gazeteci ve romancı.",
     "Radetzkymarsch", 1932, 432, "Roman", "Kiepenheuer & Witsch"),
    ("Arthur Schnitzler", 1862, 1931, "Avusturya", "Viyana toplumunun psikolojisini işleyen oyun yazarı ve hekim.",
     "Traumnovelle", 1926, 112, "Öykü", "S. Fischer"),
    ("Erich Maria Remarque", 1898, 1970, "Almanya", "Birinci Dünya Savaşı'nı askerin gözünden anlatan romancı.",
     "Im Westen nichts Neues", 1929, 288, "Roman", "Kiepenheuer & Witsch"),
    ("Alfred Döblin", 1878, 1957, "Almanya", "Büyük şehir romanının öncüsü, hekim ve yazar.",
     "Berlin Alexanderplatz", 1929, 528, "Roman", "S. Fischer"),
    ("Bertolt Brecht", 1898, 1956, "Almanya", "Epik tiyatronun kurucusu oyun yazarı ve şair.",
     "Mutter Courage und ihre Kinder", 1941, 112, "Tiyatro", "Suhrkamp"),
    ("Erich Kästner", 1899, 1974, "Almanya", "Çocuk kitapları ve hicivli şiirleriyle tanınan yazar.",
     "Emil und die Detektive", 1929, 160, "Çocuk", "Dressler"),
    ("Lion Feuchtwanger", 1884, 1958, "Almanya", "Tarihi romanlarıyla tanınan, sürgünde yazan romancı.",
     "Die Geschwister Oppermann", 1933, 400, "Roman", "S. Fischer"),
    ("Anna Seghers", 1900, 1983, "Almanya", "Nazi dönemini ve sürgünü anlatan romancı.",
     "Das siebte Kreuz", 1942, 432, "Roman", "dtv"),
    ("Heinrich Böll", 1917, 1985, "Almanya", "Savaş sonrası Alman toplumunu eleştiren Nobel ödüllü yazar.",
     "Ansichten eines Clowns", 1963, 288, "Roman", "Kiepenheuer & Witsch"),
    ("Günter Grass", 1927, 2015, "Almanya", "Danzig üçlemesiyle tanınan Nobel ödüllü romancı ve ressam.",
     "Die Blechtrommel", 1959, 784, "Roman", "Steidl"),
    ("Wolfgang Borchert", 1921, 1947, "Almanya", "'Yıkıntı edebiyatı'nın simgesi, genç yaşta ölen yazar.",
     "Draußen vor der Tür", 1947, 64, "Tiyatro", "Rowohlt"),
    ("Max Frisch", 1911, 1991, "İsviçre", "Kimlik sorununu işleyen İsviçreli romancı ve oyun yazarı.",
     "Homo faber", 1957, 208, "Roman", "Suhrkamp"),
    ("Friedrich Dürrenmatt", 1921, 1990, "İsviçre", "Grotesk komedileri ve polisiyeleriyle tanınan İsviçreli yazar.",
     "Der Besuch der alten Dame", 1956, 160, "Tiyatro", "Diogenes"),
    ("Siegfried Lenz", 1926, 2014, "Almanya", "Suç ve sorumluluk temalarını işleyen romancı.",
     "Deutschstunde", 1968, 560, "Roman", "Hanser"),
    ("Christa Wolf", 1929, 2011, "Almanya", "Doğu Almanya'nın en önemli yazarlarından.",
     "Kassandra", 1983, 176, "Roman", "Suhrkamp"),
    ("Ingeborg Bachmann", 1926, 1973, "Avusturya", "Şiirleri ve tek tamamlanmış romanıyla tanınan Avusturyalı yazar.",
     "Malina", 1971, 368, "Roman", "Suhrkamp"),
    ("Thomas Bernhard", 1931, 1989, "Avusturya", "Avusturya'yı sert monologlarla eleştiren romancı ve oyun yazarı.",
     "Holzfällen", 1984, 336, "Roman", "Suhrkamp"),
    ("Peter Handke", 1942, None, "Avusturya", "Dil ve algı üzerine yazan Nobel ödüllü Avusturyalı yazar.",
     "Die Angst des Tormanns beim Elfmeter", 1970, 128, "Roman", "Suhrkamp"),
    ("Patrick Süskind", 1949, None, "Almanya", "Koku üzerine yazdığı dünya çapında çok satan romanın yazarı.",
     "Das Parfum", 1985, 320, "Roman", "Diogenes"),
    ("Michael Ende", 1929, 1995, "Almanya", "Momo ve Bitmeyen Öykü ile tanınan fantastik çocuk edebiyatı yazarı.",
     "Die unendliche Geschichte", 1979, 480, "Fantastik", "Thienemann"),
    ("Cornelia Funke", 1958, None, "Almanya", "Mürekkep Dünyası serisiyle tanınan çocuk ve gençlik yazarı.",
     "Tintenherz", 2003, 576, "Fantastik", "Dressler"),
    ("Bernhard Schlink", 1944, None, "Almanya", "Hukukçu ve romancı; geçmişle yüzleşme temalı romanların yazarı.",
     "Der Vorleser", 1995, 208, "Roman", "Diogenes"),
    ("W. G. Sebald", 1944, 2001, "Almanya", "Hafıza ve kayıp üzerine fotoğraflı anlatılarıyla tanınan yazar.",
     "Austerlitz", 2001, 416, "Roman", "Hanser"),
    ("Herta Müller", 1953, None, "Almanya", "Romanya doğumlu, diktatörlük altındaki yaşamı anlatan Nobel ödüllü yazar.",
     "Atemschaukel", 2009, 304, "Roman", "Hanser"),
    ("Daniel Kehlmann", 1975, None, "Almanya", "Tarih ve bilimi mizahla işleyen çağdaş romancı.",
     "Die Vermessung der Welt", 2005, 304, "Roman", "Rowohlt"),
    ("Juli Zeh", 1974, None, "Almanya", "Toplum ve hukuk üzerine yazan çağdaş romancı ve hukukçu.",
     "Unterleuten", 2016, 640, "Roman", "dtv"),
    ("Elfriede Jelinek", 1946, None, "Avusturya", "Toplumsal cinsiyet ve iktidarı eleştiren Nobel ödüllü yazar.",
     "Die Klavierspielerin", 1983, 288, "Roman", "Rowohlt"),
    ("Uwe Timm", 1940, None, "Almanya", "Savaş sonrası Almanya'yı anlatan romancı.",
     "Die Entdeckung der Currywurst", 1993, 192, "Roman", "dtv"),
    ("Wolfgang Herrndorf", 1965, 2013, "Almanya", "Gençlik romanıyla geniş kitlelere ulaşan yazar ve ressam.",
     "Tschick", 2010, 256, "Roman", "Rowohlt"),
    ("Sebastian Fitzek", 1971, None, "Almanya", "Almanya'nın en çok satan psikolojik gerilim yazarı.",
     "Die Therapie", 2006, 336, "Gerilim", "Droemer"),
    ("Immanuel Kant", 1724, 1804, "Almanya", "Aydınlanma çağının en etkili filozofu.",
     "Kritik der reinen Vernunft", 1781, 850, "Felsefe", "Reclam"),
    ("Karl Marx", 1818, 1883, "Almanya", "Kapitalizmin eleştirisini yapan filozof ve iktisatçı.",
     "Das Kapital", 1867, 900, "Felsefe", "dtv"),
    ("Arthur Schopenhauer", 1788, 1860, "Almanya", "Kötümser felsefesiyle tanınan Alman filozof.",
     "Die Welt als Wille und Vorstellung", 1819, 700, "Felsefe", "dtv"),
    ("Gotthold Ephraim Lessing", 1729, 1781, "Almanya", "Hoşgörüyü savunan Aydınlanma dönemi oyun yazarı ve eleştirmen.",
     "Nathan der Weise", 1779, 176, "Tiyatro", "Reclam"),
    ("Novalis", 1772, 1801, "Almanya", "Alman romantizminin öncü şairi.",
     "Heinrich von Ofterdingen", 1802, 240, "Roman", "Reclam"),
]


def call(method, path, body=None, token=None):
    h = {"Content-Type": "application/json"}
    if token:
        h["Authorization"] = "Bearer " + token
    req = urllib.request.Request(BASE + path, data=json.dumps(body).encode() if body is not None else None,
                                 method=method, headers=h)
    try:
        with urllib.request.urlopen(req, timeout=15) as r:
            text = r.read().decode()
    except urllib.error.HTTPError as e:
        raise SystemExit(f"{method} {path} -> {e.code}: {e.read().decode()[:300]}")
    return json.loads(text) if text else None


def main():
    assert len(DATA) == 100 and len({d[0] for d in DATA}) == 100, "expected 100 distinct authors"
    token = call("POST", "/api/auth/login", ADMIN)["token"]
    rnd = random.Random(42)  # same copy counts on every run

    publishers = {p["name"]: p["id"] for p in call("GET", "/api/publishers", token=token)}
    for name, city in PUBLISHERS.items():
        if name not in publishers:
            publishers[name] = call("POST", "/api/publishers", {"name": name, "city": city}, token)["id"]

    authors = {a["name"]: a["id"] for a in call("GET", "/api/authors", token=token)}
    titles = {b["title"] for b in call("GET", "/api/books", token=token)}
    new_authors = new_books = 0
    for name, born, died, nationality, bio, title, year, pages, category, publisher in DATA:
        if name not in authors:
            authors[name] = call("POST", "/api/authors", {"name": name, "birthYear": born, "deathYear": died,
                                                          "nationality": nationality, "biography": bio}, token)["id"]
            new_authors += 1
        copies = rnd.randint(1, 4)
        if title not in titles:
            call("POST", "/api/books", {"title": title, "authorId": authors[name], "publisherId": publishers[publisher],
                                        "pageCount": pages, "category": category, "publishYear": year,
                                        "totalCopies": copies}, token)
            new_books += 1
    print(f"added {new_authors} authors and {new_books} books "
          f"(catalog now: {len(call('GET', '/api/authors', token=token))} authors, "
          f"{len(call('GET', '/api/books', token=token))} books)")


if __name__ == "__main__":
    main()
