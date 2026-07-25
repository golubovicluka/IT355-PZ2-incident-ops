# Ugovor registracije

IncidentOps podržava javno kreiranje naloga kroz `POST /register`. Zahtev
prihvata `displayName`, `username` i `password`; potvrda lozinke je isključivo
klijentsko polje i ne šalje se serveru.

## Politika naloga

- nalog je aktivan odmah i ne prolazi dodatno odobravanje;
- početna i jedina uloga je `RESPONDER`;
- nalog se dodeljuje postojećem timu `Incident Response`;
- javni zahtev ne može da izabere ili kreira ulogu ili tim;
- lokalni profil unapred obezbeđuje registracioni tim kroz demonstracione
  podatke, dok druga okruženja moraju da ga obezbede administrativnim procesom.

Ako registracioni tim ne postoji, server ne menja katalog timova i vraća
strukturisan `503` sa neutralnom porukom. Korisničko ime se normalizuje na mala
slova, a lozinka se čuva samo kao BCrypt heš. Duplikat korisničkog imena vraća
`409` i `fieldErrors.username`; odgovor nikada ne sadrži lozinku ili heš.

Uspešan frontend tok vodi korisnika na `/sign-in` sa potvrdom, nakon čega se
korisnik prijavljuje novim kredencijalima. Forma ostaje zaključana dok zahtev
traje, mapira serverske greške na odgovarajuća polja i prikazuje neutralnu
poruku za neočekivane greške.
