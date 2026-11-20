# SecureChat — Java TCP/IP Encrypted Chat (Caesar cipher)

University Cryptography project — ENP (National Polytechnic School).

> **The Caesar cipher is an educational demonstration only.** It has just 25 usable keys and can be broken in seconds (brute force or frequency analysis). Never use it to protect real confidential data.

---

## 1. Project overview

SecureChat lets several people on **different computers connected to the same Wi-Fi** chat in real time.

- **Server** — runs on one computer. It accepts connections and forwards messages. It **never decrypts** anything.
- **Client** — a Swing window that runs on every user's computer. It encrypts what you type and decrypts what you receive.

Only the Java standard library is used (JDK 25, Swing, `Socket`, `ServerSocket`, `Thread`, `DataInputStream`/`DataOutputStream`). No Maven, Gradle, JavaFX, database or cloud service.

---

## 2. Architecture and project structure

```
SecureChat/
├── README.md
├── scripts/                      PowerShell helpers (optional)
│   ├── compile.ps1
│   ├── run-server.ps1
│   ├── run-client.ps1
│   └── open-firewall.ps1
├── src/securechat/
│   ├── common/                   used by BOTH programs
│   │   ├── CaesarCipher.java
│   │   └── Protocol.java
│   ├── server/                   the server program
│   │   ├── ChatServer.java       (main class)
│   │   └── ClientHandler.java
│   └── client/                   the client program
│       ├── ChatClient.java       (main class)
│       ├── MainFrame.java
│       ├── ConnectionPanel.java
│       ├── ChatPanel.java
│       ├── ServerConnection.java
│       └── ReceiverThread.java
└── test/securechat/common/
    └── CaesarCipherTest.java     small self-test for the cipher
```

```
   Client A (Yasmine)                Server (192.168.1.15:5000)               Client B (Arsou)
 ┌─────────────────┐           ┌──────────────────────────────┐          ┌─────────────────┐
 │ types "Hello"   │           │  ClientHandler thread (yasmine)│          │                 │
 │ encrypt → "Khoor"│──TCP────▶│  reads frame                 │          │                 │
 │                 │           │  ChatServer.broadcast(...)   │──TCP────▶│ ReceiverThread  │
 │                 │           │  (still "Khoor", never        │          │ decrypt → "Hello"│
 │                 │◀──TCP─────│   decrypted)                 │          │ shows in window │
 └─────────────────┘           └──────────────────────────────┘          └─────────────────┘
```

---

## 3. Purpose of every Java class (simple explanations)

| Class              | Package | What it does                                                                                                                                                                                                                   |
| ------------------ | ------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `CaesarCipher`     | common  | The encryption tool. `encrypt(text, shift)` turns letters into other letters; `decrypt(text, shift)` turns them back.                                                                                                          |
| `Protocol`         | common  | The "rulebook" both programs share: message type names (`JOIN`, `MSG`…), the default port 5000, the default key 3, size limits and the timestamp format. Having it in one place stops client and server from disagreeing.      |
| `ChatServer`       | server  | The server's `main`. Opens port 5000, waits for clients, starts one `ClientHandler` thread per client, keeps the list of connected clients, and broadcasts messages. Also prints the computer's IPv4 addresses when it starts. |
| `ClientHandler`    | server  | The "waiter" dedicated to **one** client. Checks the username, reads that client's messages, hands them to the server for broadcasting, and cleans up when the client leaves or crashes.                                       |
| `ChatClient`       | client  | The client's `main`. Just opens the window.                                                                                                                                                                                    |
| `MainFrame`        | client  | The window. Switches between the connection screen and the chat screen, and connects the buttons to the network code.                                                                                                          |
| `ConnectionPanel`  | client  | Screen 1: server IP, port, username, shared key, **Connect** button, status line.                                                                                                                                              |
| `ChatPanel`        | client  | Screen 2: conversation area, message box, **Send**, **Disconnect**, status line.                                                                                                                                               |
| `ServerConnection` | client  | Owns the socket. Connects, joins, encrypts and sends messages (in a background thread), and disconnects.                                                                                                                       |
| `ReceiverThread`   | client  | A background thread that waits for incoming messages, decrypts them and gives them to the window.                                                                                                                              |
| `CaesarCipherTest` | test    | Checks the cipher on many inputs.                                                                                                                                                                                              |

---

## 4. Client-server communication flow

1. **Server starts** → `new ServerSocket(5000)` (listens on all network interfaces).
2. **Client connects** → `Socket.connect(serverIP, 5000)`, then sends `JOIN` + username.
3. **Server checks the name.** If valid and unused → replies `OK`; otherwise `ERR` + reason and closes the connection.
4. **Server announces** `SYS "yasmine joined the chat"` to everybody.
5. **yasmine types a message** → the client encrypts it → sends `MSG, username, timestamp, encrypted`.
6. **Server broadcasts** the same `MSG` frame to _all_ clients (including yasmine, so she sees what the network really delivered).
7. **Each client's `ReceiverThread`** decrypts and shows `[14:05:09] yasmine: Hello`.
8. **yasmine clicks Disconnect** → client sends `LEAVE` and closes; server announces `SYS "yasmine left the chat"`.

---

## 5. Threading and concurrency

A _thread_ is a separate line of execution, so one slow job does not freeze the others.

| Thread                                  | Where  | Job                                                                                                                        |
| --------------------------------------- | ------ | -------------------------------------------------------------------------------------------------------------------------- |
| Main thread                             | server | Loops on `accept()`; starts a handler thread for every new client.                                                         |
| `ClientHandler` thread (one per client) | server | Blocks on `readUTF()` waiting for that client's messages.                                                                  |
| Swing **Event Dispatch Thread (EDT)**   | client | Draws the window and runs button clicks. **Never blocked.**                                                                |
| `SwingWorker` thread                    | client | Performs the connection attempt (can take seconds) so the window stays responsive.                                         |
| `sender-thread`                         | client | A single-thread executor that writes messages to the socket. Because it is one thread, messages cannot get mixed together. |
| `ReceiverThread`                        | client | Blocks on `readUTF()` waiting for messages from the server.                                                                |

How the rules are respected:

- **Thread-safe client list:** `CopyOnWriteArrayList<ClientHandler>`; username registration is `synchronized` so two people cannot take the same name at the same moment.
- **Thread-safe writes:** every `ClientHandler.send()` uses `synchronized (writeLock)`, because several threads may broadcast to the same client at once. A frame (type + fields) is written and flushed as one unit.
- **GUI updates from background threads:** `ReceiverThread` never touches Swing directly; `MainFrame` wraps every GUI change in `SwingUtilities.invokeLater(...)`.
- **Errors never crash the server:** each handler catches `EOFException`, `SocketTimeoutException`, `IOException` and `RuntimeException`, and its `finally` block always removes the client and closes the socket.

> Limitation (fine for a class project): a client that stops reading entirely could slow broadcasting, because writes are done in sequence.

---

## 6. Caesar encryption and decryption

Each letter moves `shift` positions in the alphabet (wrapping around after Z). With shift 3: `A→D`, `B→E`, … `X→A`, `Y→B`, `Z→C`.

```
Hello, World! 123   --encrypt(shift 3)-->   Khoor, Zruog! 123
```

- Works on `A–Z` and `a–z`. Spaces, digits, punctuation and accented characters (é, ç, à…) are left unchanged.
- Negative shifts and shifts above 25 work (`shift mod 26` is used; 29 behaves like 3, −3 like 23).
- `decrypt(encrypt(x, k), k) == x` for every text and every key.
- The shared key is typed in the **"Shared key (shift)"** box on the connection screen (default `3`). **All users must use the same number**; if someone uses another key they will see unreadable text (this is a nice thing to demonstrate in class).

**Encryption workflow:** sender types plaintext → client encrypts → server relays without decrypting → each client decrypts → GUI displays plaintext.

---

## 7. Communication protocol

Transport: TCP. Every field is one string written with `writeUTF()` and read with `readUTF()`. A _frame_ is a type string followed by the fields listed below, **in exactly this order**.

**Client → Server**

| Type    | Fields in order                                | Meaning                                               |
| ------- | ---------------------------------------------- | ----------------------------------------------------- |
| `JOIN`  | `"JOIN"`, username                             | First frame after connecting.                         |
| `MSG`   | `"MSG"`, username, timestamp, encryptedContent | A chat message. Only `encryptedContent` is encrypted. |
| `LEAVE` | `"LEAVE"`                                      | Polite goodbye.                                       |

**Server → Client**

| Type  | Fields in order                                | Meaning                                          |
| ----- | ---------------------------------------------- | ------------------------------------------------ |
| `OK`  | `"OK"`                                         | `JOIN` accepted.                                 |
| `ERR` | `"ERR"`, reason                                | `JOIN` refused; server closes the connection.    |
| `MSG` | `"MSG"`, username, timestamp, encryptedContent | Broadcast chat message.                          |
| `SYS` | `"SYS"`, text                                  | Server notice (someone joined/left). Plain text. |

Example conversation:

```
C → S : "JOIN" "yasmine"
S → C : "OK"
C → S : "MSG" "yasmine" "14:05:09" "Khoor"
S → * : "MSG" "yasmine" "14:05:09" "Khoor"        (broadcast)
C → S : "LEAVE"
```

Rules and error handling:

- Username: 1–20 characters, no control characters, must be unique (case-insensitive). The server uses _its own record_ of the username, so nobody can pretend to be someone else.
- Plaintext messages ≤ 500 characters (`writeUTF` allows at most 65 535 bytes); the server drops encrypted contents longer than 2 000 characters or empty ones.
- **Unknown frame type** → the server can no longer tell where the next frame starts, so it closes that client's connection (others are not affected).
- Unexpected disconnects, timeouts (10 s handshake limit) and I/O errors are caught on both sides; the client returns to the connection screen with a message.

---

## 8. Compilation and execution (Windows PowerShell)

### Requirements

JDK 25 installed (`javac` and `java` must work). Check:

```powershell
java -version
javac -version
```

### Compile

Open PowerShell in the project folder (replace the path with yours):

```powershell
cd C:\Users\YourName\Documents\SecureChat
New-Item -ItemType Directory -Force out | Out-Null
javac -encoding UTF-8 -d out (Get-ChildItem -Path src -Recurse -Filter *.java | ForEach-Object FullName)
```

No output means success. (Shortcut: `.\scripts\compile.ps1` — if PowerShell blocks scripts, first run `Set-ExecutionPolicy -Scope Process Bypass`.)

### Optional: test the cipher

```powershell
javac -encoding UTF-8 -cp out -d out test\securechat\common\CaesarCipherTest.java
java -cp out securechat.common.CaesarCipherTest
```

Expected: `All Caesar tests passed.`

### Start the server (computer 1)

```powershell
java -cp out securechat.server.ChatServer
```

It prints something like:

```
[14:00:01] SecureChat server listening on port 5000 (all network interfaces)
[14:00:01]    192.168.1.15   (Wi-Fi)
```

Leave this window open. (Another port: `java -cp out securechat.server.ChatServer 5001`.)

### Start a client (every computer, including the server's)

```powershell
java -cp out securechat.client.ChatClient
```

Run the command again in another PowerShell window to open more clients on the same computer.

---

## 9. Connecting two computers over Wi-Fi

### 9.1 Find the server's IPv4 address

On the **server computer**:

```powershell
ipconfig
```

Look under **Wireless LAN adapter Wi-Fi** for **IPv4 Address**, e.g. `192.168.1.15`. Ignore addresses of VirtualBox/VMware/WSL/Hyper-V adapters — they are not your Wi-Fi.

Quick way: `(Get-NetIPAddress -AddressFamily IPv4 -InterfaceAlias "Wi-Fi").IPAddress`

### 9.2 Connect

1. Both computers on the **same Wi-Fi**.
2. Start the server on computer 1.
3. On computer 2, start the client and enter: **Server IPv4** `192.168.1.15`, **Port** `5000`, any **Username**, **Shared key** `3` → **Connect**.

(The code never hard-codes `localhost`; the address always comes from the text field. On the _same_ computer as the server you can type `127.0.0.1` for a quick test.)

---

## 10. Firewall configuration (Windows Defender Firewall)

The **server** computer must allow incoming connections on TCP port 5000. Clients need nothing (outgoing connections are allowed by default).

### Option A — PowerShell (recommended, one command)

Open PowerShell **as Administrator** (right-click → _Run as administrator_):

```powershell
New-NetFirewallRule -DisplayName "SecureChat Server (TCP 5000)" -Direction Inbound -Protocol TCP -LocalPort 5000 -Action Allow -Profile Private
```

Remove it later:

```powershell
Remove-NetFirewallRule -DisplayName "SecureChat Server (TCP 5000)"
```

### Option B — Allow Java through the firewall (graphical)

1. Start → type **Windows Defender Firewall** → _Allow an app or feature through Windows Defender Firewall_.
2. _Change settings_ → _Allow another app…_ → _Browse_ → choose `java.exe` (inside your JDK folder, `bin\java.exe`).
3. Tick **Private** for it → OK.

(When the server starts, Windows may also show a pop-up "Allow access?" — tick **Private networks** and click _Allow access_.)

### Is the Wi-Fi "Private"?

If Windows treats your Wi-Fi as **Public**, the rule above (Private only) will not apply. Check and change it:

```powershell
Get-NetConnectionProfile
Set-NetConnectionProfile -InterfaceAlias "Wi-Fi" -NetworkCategory Private   # as Administrator
```

---

## 11. Testing and troubleshooting

### 11.1 Test the network (before starting the chat clients)

With the server running, on the **client computer**:

```powershell
ping 192.168.1.15
Test-NetConnection -ComputerName 192.168.1.15 -Port 5000
```

You want `TcpTestSucceeded : True`.

- `ping` fails → not on the same network, wrong IP, or the router isolates devices.
- `ping` works but `TcpTestSucceeded : False` → the server is not running, or the **firewall** is blocking port 5000.

### 11.2 Test communication between two clients

1. Start the server.
2. Start **client 1**: IP = server IP, username `yasmine`, key `3` → Connect.
3. Start **client 2** (other computer, or a second PowerShell window): username `arsou`, key `3` → Connect.
4. yasmine sees `* arsou joined the chat`.
5. yasmine types `Hello arsou, ça va ?` and presses **Enter**.
6. Both windows show `[time] yasmine: Hello arsou, ça va ?`.
7. Look at the **server window**: it logs joins and leaves but never shows message text — it only relays encrypted data.
8. Try a third client `Carol` with **key 7**: she receives yasmine's message as unreadable text, proving the key matters.
9. Try a client named `yasmine` → rejected: _Username 'yasmine' is already in use_.
10. Click **Disconnect** → the others see `* arsou left the chat`.

### 11.3 Common problems

| Symptom                                          | Likely cause and fix                                                                                                                                                                         |
| ------------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `Connection timed out`                           | Wrong IP, different Wi-Fi, or firewall. Redo section 11.1.                                                                                                                                   |
| `Connection refused`                             | Server not running, or wrong port.                                                                                                                                                           |
| Works on server PC but not from others           | Firewall (section 10), or Wi-Fi marked **Public**.                                                                                                                                           |
| `ping` works but chat does not                   | Firewall rule missing for port 5000.                                                                                                                                                         |
| Nothing works between computers; same Wi-Fi      | Many university / public networks enable **client isolation** (devices cannot see each other). Use a phone **hotspot** or a home router for the demo.                                        |
| Wrong IP address                                 | You picked a virtual adapter (VirtualBox, VMware, WSL). Use the **Wi-Fi** adapter's IPv4. The IP can also change after reconnecting — re-check with `ipconfig`.                              |
| `Cannot start server ... Address already in use` | Another program (or an old server window) uses port 5000. Find it: `netstat -ano \| findstr :5000`, then close it or use another port (`... ChatServer 5001`, and type 5001 in the clients). |
| `Username ... already in use`                    | Choose another name.                                                                                                                                                                         |
| Text looks scrambled                             | Different **Shared key** on some clients; everyone must type the same number.                                                                                                                |
| `'javac' is not recognized`                      | JDK not installed or not in `PATH`; reopen PowerShell after installing.                                                                                                                      |
| VPN active                                       | Disconnect the VPN, it can hide the local network.                                                                                                                                           |
| Accents show as `?` in PowerShell                | Only a console display issue for the _server log_; the chat window is not affected.                                                                                                          |

---

## Verification notes

This project was compiled together without errors and tested automatically: cipher round trips (including negative and huge shifts), a protocol test with several raw clients (join, duplicate name, broadcast of ciphertext untouched, malformed frame, `LEAVE`, abrupt disconnect), and a test that operated the real Swing windows (three clients, wrong key, disconnect, server crash, connection refused). The tests ran on JDK 21 (the version available where this was built). The code uses only long-standing standard APIs, so it should compile unchanged on JDK 25.
