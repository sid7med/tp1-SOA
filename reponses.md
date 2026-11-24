# TP1 SOAP et WSDL — réponses

Projets : `emprunt-codefirst` (partie A) et `emprunt-contractfirst` (partie B).  
Messages : dossier `xml/`.

## Étape 1 — Créer et lancer le service (code-first)

**1. Quelle annotation fait de `EmpruntService` un service web ? Quelle annotation fait de `LivreIndisponibleException` une erreur déclarée dans le contrat ?**

- `@WebService` (avec `serviceName`, `portName`, `targetNamespace`) publie la classe comme endpoint SOAP.
- `@WebFault(name = "LivreIndisponible", targetNamespace = "http://supnum.mr/bib")` déclare l’exception comme faute métier du contrat (élément `LivreIndisponible` dans le WSDL).

**2. Pourquoi les classes de données doivent-elles avoir un constructeur vide ?**

JAXB / JAX-WS instancie `Emprunt` et `LivreIndisponibleInfo` par réflexion lors du (dé)marshalling XML. Sans constructeur public sans argument, la (dé)sérialisation échoue.

## Étape 2 — Lire le WSDL généré

**1. Quelle est l’URL du service ? Quelles sont les deux opérations et leurs messages ?**

- URL : `http://localhost:8080/bib/EmpruntService`  
  WSDL : `http://localhost:8080/bib/EmpruntService?wsdl`
- `emprunterLivre` : entrée `emprunterLivre` / sortie `emprunterLivreResponse` (élément `emprunt`).
- `rendreLivre` : entrée `rendreLivre` / sortie `rendreLivreResponse` (élément `penalite`).

**2. Style, use, version SOAP ?**

Binding **document / literal**, transport HTTP. JAX-WS publie ici du **SOAP 1.1** (`http://schemas.xmlsoap.org/soap/envelope/` et binding `wsdl/soap`, pas `soap12`).

**3. Types XSD pour `isbn` et `dateRetour` ? La règle ISBN apparaît-elle ?**

En code-first, les deux sont en général des `xs:string` (champs Java `String`). Le motif `97[89]-\d-…` est **dans le Java** (`Pattern`), pas dans le XSD généré. Le contrat publié ne force donc pas le format ISBN.

**4. Préfixe d’espace de noms sur `idAdherent` et `isbn` dans SoapUI ?**

Non, en wrapped document/literal code-first les enfants du wrapper sont en général **non qualifiés** (pas de préfixe). Seul l’élément d’opération (`emprunterLivre`) porte le namespace `http://supnum.mr/bib`.

**5. Onglet Raw : HTTP, XML, SOAP, WSDL ?**

- **HTTP** : transport (`POST`, `Content-Type`, `SOAPAction`, codes 200/500).
- **XML** : syntaxe du document.
- **SOAP** : enveloppe (`Envelope` / `Header` / `Body` / `Fault`).
- **WSDL** : contrat (où appeler, quelles opérations, quels types). Il n’apparaît pas dans le Raw d’un appel ; il sert à *construire* cet appel.

## Étape 3 — Envoyer des messages SOAP

**1. Pourquoi POST même pour « lire » une information ?**

SOAP/HTTP document-literal envoie une enveloppe XML dans le corps. GET n’est pas le binding habituel (pas de sémantique REST). POST transporte le message, qu’il s’agisse d’un emprunt ou d’une consultation.

**2. Le bloc `jeton` du Header a-t-il empêché le traitement ?**

Non, tant que `mustUnderstand` est absent ou à `0`. Le service ne déclare pas ce header : SOAP 1.1 autorise à l’ignorer.

**3. Que répond le service au message SOAP 1.2 ?**

Rejet de version (faute `VersionMismatch`) : le binding lu à l’étape 2 est SOAP 1.1. Une enveloppe `http://www.w3.org/2003/05/soap-envelope` n’est pas celle attendue (`xml/02-emprunter-12.xml`).

**4. Cinq erreurs du dernier message, corrections, et celles qui ne cassent pas forcément le serveur**

Message volontairement faux : `xml/04-rendre-erreurs.xml`. Version corrigée : `xml/05-rendre-ok.xml`.

| # | Erreur | Correction |
|---|--------|------------|
| 1 | `soap:envelope` (minuscule) | `soap:Envelope` (XML est sensible à la casse) |
| 2 | Namespace `http://supnum.mr/bibliotheque` | `http://supnum.mr/bib` |
| 3 | Paramètre `numero` | `numeroEmprunt` (nom `@WebParam`) |
| 4 | Balise fermante `</rendreLivre>` sans préfixe / mal appariée | `</bib:rendreLivre>` |
| 5 | `Header` **après** `Body` | `Header` puis `Body` (ordre SOAP 1.1) |

Souvent **sans erreur applicative côté métier** (selon le parseur) :

- le **jeton** (header ignoré si `mustUnderstand=0`) — ce n’est pas une des cinq fautes de structure, mais il ne bloque pas ;
- un **mauvais nom de paramètre** peut arriver jusqu’au Java avec `numeroEmprunt == null`, d’où une Fault `Client` « Numéro d’emprunt inconnu » plutôt qu’un XML mal formé.

Les erreurs de **well-formedness** (casse de `Envelope`, balises mal appariées, Header mal placé selon le parseur) sont rejetées **avant** le métier.

## Étape 4 — Faults

**1. Cas ISBN `978-2-10` et livre déjà sorti `978-2-10-074444-4`**

- Cas 1 (format invalide) : `faultcode` **Client**, `faultstring` du type `ISBN mal formé : 978-2-10`, HTTP **500**.
- Cas 2 (indisponible) : faute **déclarée** `LivreIndisponible`, `faultstring` `Livre indisponible`, HTTP **500**.

**2. Contenu de `<detail>` au cas 2 ? Où est-ce décrit ?**

`<detail>` contient l’élément `LivreIndisponible` (`isbn`, `retourPrevu`). Structure : `@WebFault` + `LivreIndisponibleInfo`, messages `fault` du `portType` / `binding` dans le WSDL (et le XSD `?xsd=1`).

**3. Code choisi par JAX-WS pour le livre indisponible ?**

Faute d’application mappée sur le contrat (`LivreIndisponible`), souvent avec un `faultcode` **Server** (exception levée par le service) alors que la cause est bien une demande client (livre déjà emprunté). Un `Client` / `Sender` serait plus juste sémantiquement.

**4. `mustUnderstand="1"` vs `"0"` sur `<bib:jeton>`**

- `"1"` : le header n’est pas compris → Fault **MustUnderstand**, appel non traité.
- `"0"` ou absent : header ignoré, traitement normal.

**5. Différence Fault SOAP 1.2 écrite à la main vs Faults 1.1 reçues**

SOAP 1.1 : `faultcode`, `faultstring`, `faultactor` optionnel, `detail`.  
SOAP 1.2 (`xml/03-fault-sender-12.xml`) : `Code/Value` (`soap:Sender`), `Reason/Text` avec `xml:lang`, `Detail`. Vocabulaire Client/Server (1.1) vs Sender/Receiver (1.2).

## Étape 5 — Contract-first / WSDL

**1. Opération, fault, SOAPAction**

- Opération : `emprunterLivre`
- Fault : `LivreIndisponible` / message `LivreIndisponibleFault`
- SOAPAction : `http://supnum.mr/bib/emprunterLivre`

**2. Signature dans `EmpruntPortType` (wsimport)**

`EmpruntType emprunterLivre(String idAdherent, String isbn) throws LivreIndisponibleFault`

**3. Type Java de `dateRetour` vs partie A**

Partie B : `XMLGregorianCalendar` (XSD `xs:date`). Partie A : `String`.

**4. Nom de l’exception générée ?**

`LivreIndisponibleFault` — dérivé du **message** WSDL `LivreIndisponibleFault` (pas seulement de l’élément `LivreIndisponible`).

## Étape 6 — Implémenter et comparer

**1. Le WSDL publié est-il le nôtre ? Motif ISBN et `xs:date` ?**

Avec `wsdlLocation = "wsdl/EmpruntService.wsdl"`, l’endpoint sert **le contrat fourni** : oui, motif `IsbnType` et `dateRetour` en `xs:date`.

**2. Faut-il modifier `01-emprunter.xml` ?**

Le Body `emprunterLivre` + `idAdherent` / `isbn` reste valable. Il faut surtout l’en-tête **SOAPAction** `http://supnum.mr/bib/emprunterLivre` (vide `""` suffisait en code-first SOAP 1.1). Si le schéma est `elementFormDefault="qualified"`, SoapUI/Metro peuvent exiger des enfants préfixés (`bib:idAdherent`, `bib:isbn`).

**3. Sans / avec `@SchemaValidation` pour ISBN `978-2-10`**

- Sans : le Java de la partie B ne valide pas le motif ; `978-2-10` n’est pas le livre bloqué → emprunt accepté à tort.
- Avec : le runtime valide le XSD → Fault (souvent Sender/Client) pour violation du `pattern` `IsbnType`.  
  L’annotation est déjà sur `EmpruntServiceImpl` pour cette démo.

**4. Sans `wsdlLocation`**

JAX-WS **regénère** un WSDL depuis l’interface Java : on peut perdre le `pattern` ISBN, `soapAction` fixé, et parfois `xs:date` au profit de types Java par défaut. Ce n’est plus le fichier écrit à la main.

## Conclusion — Choisir et rendre

**1. Deux différences A vs B**

1. A : `isbn` / `dateRetour` en `xs:string`, **sans** motif ISBN ; B : `IsbnType` + `xs:date`.
2. A : deux opérations (`emprunterLivre`, `rendreLivre`), SOAPAction souvent vide ; B : une opération, SOAPAction obligatoire, fault `LivreIndisponible` écrite dans le WSDL.

**2. Renommer `dateRetour` dans le code**

Seul le **code-first** change le contrat (WSDL généré). Les clients déjà générés cassent. En contract-first, le WSDL reste la référence : le Java généré / l’implémentation doivent suivre le contrat, pas l’inverse.

**3. Paiement multi-équipes vs prototype**

- Paiement appelé par .NET, PHP et Node.js → **contract-first** (contrat partagé, stable, indépendant du langage).
- Prototype interne de deux jours → **code-first** (plus rapide), puis figer le WSDL avant tout vrai consommateur.
