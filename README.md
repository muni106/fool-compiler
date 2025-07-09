# LCMC domande

- Cosa produce in output una symbol table?

  Nella nostra implementazione di symbol table come **lista di mappe**, andiamo ad aggiungere una mappa per ogni scope attualmente visibile.

  L’**obiettivo** di una symbol table è quello di tenere traccia dei nomi dichiarati in un programma. Le dichiarazioni nel nostro caso sono variabili, classi, campi di una classe (fields) e metodi/funzioni.

  Le Symbol Table Entry sono set di attribute associate a un nome di una dichiarazione.

    - Nel nostro programma non possiamo avere più dichiarazioni della stessa variabile nello stesso scope.
    - I nomi devono essere necessariamente dichiarati prima di essere utilizzati

  La sybol table **serve** finche:

    - Tutte le dichiarazioni per costruire la symbol table sono state processate
    - Tutti gli usi delle dichiarazioni sono stati processati in modo tale da collegare la STEntry della dichiarazione al nodo dell’uso nell’AST

  Dopodichè la sybol table non serve più.

- Cosa produce **SybolTableASTVisitor**?

  SymbolTableASTVisitor produce L’enriched AST che è un AST arricchito nel senso che ogni nodo in cui abbiamo un uso di un ID andiamo a collegarlo alla STEntry della dichiarazione associata (quella dello scope più vicino).

- Qual è la differenza tra symbol table e class table?

  La class table
  • Mappa ogni nome di classe alla corrispondente virtual table
  • serve per tenere traccia delle dichiarazioni interne ad una classe (campi e metodi) rendendole accessibili in seguito tramite il nome della classe ad esempio con la notazione dot.
  • Serve anche per calcolare/ereditare virtual table della super classe.

- A cosa serve il type checking?

  Il type checking fa parte della **fase di analisi semantica** del compilatore, eseguita prima della generazione di codice.
  Serve a **verificare**, sull’AST arricchito con i puntatori alle entry di symbol/class/virtual table, che ogni espressione, dichiarazione o invocazione rispetti le regole del sistema di tipi del linguaggio.

  Errori che intercetta:

    - **incompatibilità tra tipi** negli operatori (tipo sommare un bool a un int)
    - numero o tipo errato di argomenti nelle chiamate di funzione/metodo
    - controllo della covarianza e controvarianza nei metodi e funzioni
- Come funziona ereditarietà nel type checking?

  Il metodo statico **`isSubtype`** è esteso così:

    - **`Ref A`** ≤ **`Ref B`** se, seguendo i collegamenti in **`superType`**, B è un antenato di A.
    - **`Empty`** (cioè **`null`**) è sottotipo di qualunque riferimento.
    - Per i tipi funzionali **`Arrow`** vale la controvarianza sui parametri e la covarianza sul risultato, regola necessaria per l’override dei metodi.

  Questa relazione è sfruttata anche da **`lowestCommonAncestor`**, usata per scegliere il tipo risultante di un **`if-then-else`** quando i due rami restituiscono riferimento a classi diverse

- Come si può dire che una classe è sottotipo di un’altra?
    - Subtyping vuol dire che se S è sottotipo di T (scritto S ≤ T), allora valori di tipo S possono essere usati dove ci si aspettano valori di tipo T **(principio di sostituzione di Liskov)**
        - un super tipo evidentemente non può essere messo al posto di un sup sottotipo, perchè molto probabilmente non esporrà alcuni metodi o campi esposti dal suo sotto tipo
    - nell’object oriented una sottoclasse è sottotipo della superclasse
    - Class subtyping ⇒ è possibile fare override sia di fields che di methods nel nostro compilatore
- Come funziona field/method overriding?

  L’override di campi e metodi si basa sul concetto di sottotipaggio: un tipo B è sottotipo di A se un valore di B può essere impiegato ovunque sia richiesto un valore di A

  **Field overriding:**

    - un campo può essere ridefinito in una sottoclasse solo se il suo tipo rimane compatibile con quello dichiarato nella super-classe.
    - Se i campi sono immutabili (non possono essere riassegnati dopo la creazione dell’oggetto) è sicuro renderli covarianti (nella sotto classe si può sostituire tipo originale con un sottotipo)

  **Methods overriding: (sottotipaggio tipi funzionali)**

  Il tipo di un metodo è espresso come funzione ( T1, …, Tn -> T ) dove T1, … Tn sono i tipi dei parametri e T è il tipo di ritorno

  Quindi andiamo a dire che una funzione è sottotipo di un’altra se valgono 2 condizioni (considerando classe B sottotipo di classe A):

    1. **Contravarianza** sui parametri => vuol dire che i parametri del metodo nella classe A devono poter essere usati al posto dei parametri del metodo nella classe B, questo vuol dire che (parametri in A sottotipo parametri in B)
    2. **Covarianza** del return => il ritorno del metodo nella classe B deve poter essere utilizzato al posto del ritorno del metodo nella classe A quindi (return B sottotipo return A)
- allocazione in memoria di oggetti e dispatch tables e connessione tra loro, gestione di offset per campi/metodi e realizzazione dynamic dispatch

  ### Allocazione in memoria

    - Ogni istanza è un blocco contiguo allocato partendo dall’indirizzo contenuto in **`$hp`**, che cresce verso indirizzi alti.
    - Il primo word del blocco (offset 0) è il *dispatch pointer* che punta alla dispatch table della classe.
    - I campi vengono copiati subito dopo l’allocazione, uno per word, agli offset −1, −2 … nell’ordine di dichiarazione (campi ereditati compresi).

  ### Dispatch tables nello heap

    - Per ogni classe il compilatore costruisce un array di indirizzi di metodi (*dispatch table*) e lo copia nello heap subito dopo lo spazio degli oggetti già occupato, facendo avanzare **`$hp`** di una word per entry.
    - L’entry i contiene l’etichetta MIPS del metodo con offset i; se il metodo è ereditato l’indirizzo viene semplicemente ricopiato.

  ### **Connessione oggetto ↔ dispatch table**

    1. In fase di generazione codice della classe si salva il dispatch pointer (l’indirizzo di inizio tabella) nell’activation record globale a un offset fisso, così sarà recuperabile dalle **`new`**.
    2. **`new C(...)`** copia quel valore al primo word dell’oggetto, quindi restituisce l’indirizzo di tale word come *object pointer*.

  ### Gestione degli offset

  **Campi**

    - Il compilatore mantiene un contatore che parte da −1 (o da −(n+1) se c’è una super-classe) e decre­mente a ogni nuovo campo; quel numero viene salvato nella STentry del campo.
    - L’offset non cambia nelle sottoclassi, anche in presenza di override, garantendo che il codice della super-classe continui a funzionare.

  **Metodi**

    - Un secondo contatore parte da 0 (o dalla lunghezza della tabella del padre) e si incrementa per ciascun metodo dichiarato.
    - Se un metodo override ha lo stesso nome di uno eredita­to, ne riusa l’offset e sovrascrive l’indirizzo nella nuova dispatch table; se è nuovo viene aggiunto in coda.ù

  ### Dynamic dispatch

    ```
    # e.f()  — offset di f noto a compile-time
    lw   $t0, 0($a0)     # $a0 = object pointer → carica dispatch pointer
    lw   $t1, 4($t0)     # 4 = offset di f * 4 byte
    jalr $t1             # salto indiretto; $a0 rimane il this
    ```

    - L’indirizzo effettivo di **`f`** dipende dalla tabella raggiunta ($t0$) e quindi dal tipo dinamico dell’oggetto, realizzando il *dynamic dispatch* con un solo load e un salto indiretto.
    - Poiché le sottoclassi preservano i primi N slot della tabella, ogni chiamata attraverso un tipo statico di super-classe trova sempre un indirizzo valido.

- virtual tables e class tables (motivo del loro nome e perche' sono necessarie in aggiunta alla symbol table), sottotipaggio tra tipi riferimento e layout oggetti/dispatch tables in memoria heap nel nostro compilatore

  ### Virtual Tables

    - **cosa sono**: All’interno della visita della dichiarazione di una classe, il compilatore crea un nuovo livello della symbol table che non è vuoto, ma è inizializzato copiando i simboli ereditati dalla super-classe; quel livello viene chiamato *Virtual Table*
    - **perchè virtual**: Come la *v-table* di un linguaggio compilato, la struttura contiene solo i campi e i metodi “visibili” nella classe dopo l’eventuale overriding: se un sottotipo ridefinisce un identificatore, la relativa STentry sostituisce l’entry ereditata ma ne conserva l’offset. Il livello rappresenta quindi la *vista virtuale* che il codice interno alla classe ha di sé stessa e dei propri avi.
    - **Perch serve**: Quando si esce dal corpo della classe il livello corrente verrebbe normalmente scartato; in tal caso occorrerebbe ricostruire ogni volta l’insieme di campi e metodi ereditati. La virtual table evita questa ricostruzione e, soprattutto, registra in un unico posto gli offset stabiliti per campi e metodi, necessari a:
        - verificare l’override corretto (stesso offset, tipo compatibile)
        - generare accessi O(1) in fase di code generation (campo = “base + offset”, metodo = “dispatch ptr + offset”).

  ### Class Tables

    - **cos’è**: Oltre alla symbol table multilivello, il compilatore mantiene una *Class Table* che mappa il nome di ogni classe alla sua Virtual Table definitiva
    - **perchè class**: La struttura è indicizzata dal nome della classe e contiene esattamente l’insieme dei simboli che caratterizza quella classe: di fatto è “la symbol table della classe” persistente dopo che la dichiarazione è stata chiusa.
    - Perchè serve:
        - Quando un’espressione **`ID1.ID2()`** appare dopo la chiusura della classe, il compilatore usa la class table per localizzare, in tempo costante, l’STentry di **`ID2`** all’interno della virtual table della classe di **`ID1`**
        - Serve a costruire la dispatch table durante la generazione del codice: l’ordine degli indirizzi in dispatch table deve rispecchiare l’ordine e gli offset registrati proprio nella virtual table salvata in class table

      Senza la class table, dopo aver chiuso la dichiarazione **non si avrebbe più un riferimento diretto ai simboli interni della classe**.


### Sub-typing fra tipi riferimento

Il compilatore mantiene una mappa **`superType`** che associa ogni classe alla sua super-classe diretta.

La funzione **`isSubtype(a,b)`** è estesa così:

- Tipi riferimento **`Ref A`** e **`Ref B`**: **`Ref A`** è sottotipo di **`Ref B`** se **`B`** è raggiungibile da **`A`** seguendo la catena **`superType`**.
- Il tipo **`Empty`** (valore **`null`**) è sottotipo di qualunque tipo riferimento.
- Per i metodi (ArrowType) vale covarianza sul risultato e controvarianza sui parametri, requisito usato per verificare override corretti.

È inoltre disponibile **`lowestCommonAncestor(a,b)`** che, dato un **`then`** e un **`else`**, restituisce il più vicino antenato comune dei due tipi di riferimento (o **`int/bool`** per i primitivi) per permettere *if-expression* con rami di tipi diversi

### Layout in heap

OGGETTO

Il puntatore all’oggetto (object pointer) coincide con l’indirizzo del dispatch pointer. I campi sono memorizzati con offset negativi perché lo heap cresce verso indirizzi alti e l’oggetto viene riempito “all’indietro”: prima il puntatore, poi i valori dei campi nell’ordine di dichiarazione (ereditati inclusi)

```
offset  0   dispatch pointer
offset -1   campo 1
offset -2   campo 2
...
offset -n   campo n
```

DISPATCH TABLE

La tabella è anch’essa allocata in heap, cresce in avanti e il puntatore alla tabella viene copiato in tutte le istanze della stessa classe. Durante la costruzione di una sottoclasse si copia la tabella del padre e si sovrascrivono (override) o si appendono (nuovi metodi) le entry secondo l’offset memorizzato nella virtual table.

```
offset 0    indirizzo metodo 0
offset 1    indirizzo metodo 1
...
offset m-1  indirizzo metodo m-1
```

**Collegamento oggetto–tabella**

1. **`new C(...)`** alloca prima tutti i campi nello heap, poi copia il dispatch pointer della classe **`C`** (conservato nell’AR globale) nell’header dell’oggetto e infine restituisce l’indirizzo dell’header sullo stack.
2. A run-time, per invocare un metodo a offset k:
    - si legge in un registro il valore all’indirizzo **`object+0`**;
    - si aggiunge k, si dereferenzia e si esegue un salto indiretto all’indirizzo del metodo.

Il contratto “offset stabilito a compile-time, accesso via dispatch pointer a run-time” realizza il dynamic dispatch con un singolo memory load e un salto indiretto.