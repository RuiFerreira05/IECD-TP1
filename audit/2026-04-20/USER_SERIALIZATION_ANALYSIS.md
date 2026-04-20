# User Object Serialization/Deserialization Analysis

## Summary
There are **formatting inconsistencies** between the protocol layer and persistence layer, but the implementations are internally consistent within each layer. However, there are some important observations.

---

## Issue 1: Two Different XML Formats for User Objects

### Persistence Layer (users.xsd & PersistenceManager.java)
**Format: User data stored as XML attributes**

```xml
<user id="550e8400-e29b-41d4-a716-446655440000" 
      username="alice" 
      passwordHash="hash123" 
      photo="base64encoded" 
      nationality="PT" 
      dob="1990-05-14">
    <stats>
        <match result="WON" playtime="120.5" opponent-id="..." opponent-username="bob"/>
    </stats>
</user>
```

**Code Location:** `PersistenceManager.java` (lines 154-159)
```java
userEl.setAttribute("id", user.getUserId().toString());
userEl.setAttribute("username", user.getUsername());
userEl.setAttribute("passwordHash", user.getPasswordHash());
if (user.getPhoto() != null) userEl.setAttribute("photo", user.getPhoto());
if (user.getNationality() != null) userEl.setAttribute("nationality", user.getNationality());
if (user.getDob() != null) userEl.setAttribute("dob", user.getDob().toString());
```

### Protocol Layer (protocol.xsd & XMLMessageBuilder.java)
**Format: User data stored as nested XML elements**

```xml
<user>
    <id>550e8400-e29b-41d4-a716-446655440000</id>
    <username>alice</username>
    <photo>base64encoded</photo>
    <nationality>PT</nationality>
    <dob>1990-05-14</dob>
    <stats>
        <match result="WON" playtime="120.5" opponent-id="..." opponent-username="bob"/>
    </stats>
</user>
```

**Code Location:** `XMLMessageBuilder.java` (lines 90-102)
```java
private Element userElement(Document doc, User user) {
    Element userEl = doc.createElement("user");
    userEl.appendChild(textElement(doc, "id",       user.getUserId().toString()));
    userEl.appendChild(textElement(doc, "username", user.getUsername()));
    if (user.getPhoto() != null) {
        userEl.appendChild(textElement(doc, "photo", user.getPhoto()));
    }
    userEl.appendChild(textElement(doc, "nationality", user.getNationality()));
    userEl.appendChild(textElement(doc, "dob", user.getDob().toString()));
    userEl.appendChild(playerStatsElement(doc, user.getStats()));
    return userEl;
}
```

---

## Issue 2: Deserialization Asymmetry

### Persistence Load (Reading from users.xml)
- **Format:** Reads from **attributes** (lines 90-95 in PersistenceManager.java)
- ✅ **Correctly matches** the persistence schema (users.xsd)

```java
UUID userId = UUID.fromString(el.getAttribute("id"));
String username = el.getAttribute("username");
String passwordHash = el.getAttribute("passwordHash");
String photo = el.hasAttribute("photo") ? el.getAttribute("photo") : null;
```

### Protocol Deserialization (Parsing received messages)
- **Format:** Not directly deserialized! 
- The XMLParser only parses message bodies, not user objects
- User objects are only serialized (sent) but never parsed (received) in the protocol layer
- ⚠️ **No deserialization code exists** for user objects in XMLParser.java

---

## Issue 3: Inconsistent Nullable Handling

### In Protocol Serialization (XMLMessageBuilder.java)
```java
// Line 94-96: Photo is conditionally serialized
if (user.getPhoto() != null) {
    userEl.appendChild(textElement(doc, "photo", user.getPhoto()));
}

// Line 97-98: Nationality and DOB are ALWAYS serialized (can cause NullPointerException!)
userEl.appendChild(textElement(doc, "nationality", user.getNationality()));
userEl.appendChild(textElement(doc, "dob", user.getDob().toString()));
```

### In Persistence Serialization (PersistenceManager.java)
```java
// Lines 157-159: All optional fields are conditionally serialized
if (user.getPhoto() != null) userEl.setAttribute("photo", user.getPhoto());
if (user.getNationality() != null) userEl.setAttribute("nationality", user.getNationality());
if (user.getDob() != null) userEl.setAttribute("dob", user.getDob().toString());
```

### ⚠️ **BUG FOUND**: NullPointerException Risk in XMLMessageBuilder.java
If `user.getNationality()` or `user.getDob()` are null, calling `.toString()` on null will throw `NullPointerException`.

---

## Issue 4: Password Hash Security

### Storage
- ✅ Correctly stored in persistence layer only (passwordHash in users.xml)
- ✅ Never exposed in protocol layer

### Transmission
- ✅ Never sent over protocol (correct for security)
- During LOGIN response, full user object is sent but passwordHash is excluded from XMLMessageBuilder

---

## Recommendations

### 1. **CRITICAL: Fix NullPointerException in XMLMessageBuilder.java**
Lines 97-98 need null checks:

```java
if (user.getNationality() != null) {
    userEl.appendChild(textElement(doc, "nationality", user.getNationality()));
}
if (user.getDob() != null) {
    userEl.appendChild(textElement(doc, "dob", user.getDob().toString()));
}
```

### 2. **Consider Schema Unification** (Optional)
Both schemas are valid, but they represent the same data in different formats. This is acceptable, but consider if one format might be clearer/easier to maintain.

### 3. **Add User Deserialization to XMLParser** (If needed)
Currently, no code deserializes user objects from protocol messages (only serializes them). If the protocol needs to support receiving user objects in responses, add this functionality.

### 4. **Add Validation Tests**
Create unit tests that:
- Serialize a User → Deserialize → Verify equality
- Test null field handling
- Verify schema compliance for both formats

---

## Files Affected
1. ✅ `XMLMessageBuilder.java` - **NEEDS FIX** (lines 97-98)
2. ⚠️ `protocol.xsd` - Correctly defines element-based format
3. ⚠️ `users.xsd` - Correctly defines attribute-based format
4. ⚠️ `PersistenceManager.java` - Correctly implements attribute-based format
5. ⚠️ `XMLParser.java` - No user deserialization (may be intentional)


