# 🚀 Déploiement GraalVM Native Image sur Render

## 📋 Vue d'ensemble

Ce dossier contient le Dockerfile pour déployer l'exécutable natif GraalVM sur Render.

**Caractéristiques** :
- ⚡ Démarrage : <2 secondes
- 💾 Mémoire : 50-100 MB
- 📦 Image Docker : ~150 MB
- ⏱️ Build Render : <30 secondes (pas de compilation Java)

---

## ⚙️ Étapes de Déploiement

### 1️⃣ Compiler l'exécutable natif en local

**Sur Windows** :
```bash
# Depuis la racine du projet
./build-native.bat
```

**Sur Linux/macOS** :
```bash
# Depuis la racine du projet
./build-native.sh
```

⏱️ **Durée** : 5-10 minutes (une seule fois)

✅ **Résultat** : Fichier exécutable créé dans `backend/target/backend`

---

### 2️⃣ Copier l'exécutable dans ce dossier

**Sur Windows (PowerShell)** :
```powershell
# Depuis la racine du projet
Copy-Item backend/target/backend Backend-Docker-Graalvm/backend
```

**Sur Windows (CMD)** :
```cmd
copy backend\target\backend Backend-Docker-Graalvm\backend
```

**Sur Linux/macOS** :
```bash
cp backend/target/backend Backend-Docker-Graalvm/backend
```

---

### 3️⃣ Vérifier que l'exécutable est présent

```bash
# L'exécutable doit exister ici :
Backend-Docker-Graalvm/backend
```

**Taille attendue** : ~80-120 MB

---

### 4️⃣ Commit et push vers le repository

```bash
git add Backend-Docker-Graalvm/backend
git commit -m "Add GraalVM native executable for Render deployment"
git push origin main
```

> ⚠️ **Note** : L'exécutable est volumineux (~100MB). C'est normal pour une image native.

---

### 5️⃣ Configurer Render

Dans les paramètres de votre Web Service Render :

**Build & Deploy** :
- **Root Directory** : `Backend-Docker-Graalvm`
- **Dockerfile Path** : `Dockerfile` (ou `./Dockerfile`)

**Environment Variables** :
```env
SPRING_PROFILES_ACTIVE=native,prod
PORT=8080

# Supabase (Transaction Mode)
SPRING_DATASOURCE_URL=jdbc:postgresql://[YOUR_HOST]:6543/postgres?prepareThreshold=0
SPRING_DATASOURCE_USERNAME=[YOUR_USERNAME]
SPRING_DATASOURCE_PASSWORD=[YOUR_PASSWORD]

# JWT
JWT_SECRET=[YOUR_SECRET]
JWT_EXPIRATION=86400000
```

**Health Check Path** :
```
/actuator/health
```

---

### 6️⃣ Déployer

1. Cliquez sur **"Manual Deploy"** → **"Deploy latest commit"**
2. Render va :
   - Construire l'image Docker (~30 secondes)
   - Lancer le conteneur
   - Démarrer l'application (<2 secondes)

---

## 🔄 Workflow de Mise à Jour

À chaque modification du code backend :

```bash
# 1. Recompiler l'exécutable natif
./build-native.bat   # ou .sh sur Linux/macOS

# 2. Copier le nouvel exécutable
Copy-Item backend/target/backend Backend-Docker-Graalvm/backend -Force

# 3. Commit et push
git add Backend-Docker-Graalvm/backend
git commit -m "Update native executable"
git push origin main

# 4. Render redéploie automatiquement (ou manuellement)
```

---

## 🆚 Comparaison des Options

| Aspect | Exécutable Pré-compilé | Build complet sur Render |
|--------|------------------------|--------------------------|
| **Démarrage** | <2s | <2s |
| **Build Render** | ~30s | 5-10 minutes |
| **Simplicité** | ⭐⭐⭐ Facile | ⭐ Complexe |
| **Taille repo** | +100MB | Pas d'impact |
| **Recommandé pour** | Render Free Tier | CI/CD automatique |

**Sur Render Free Tier** : Exécutable pré-compilé est **FORTEMENT RECOMMANDÉ** car :
- ✅ Évite timeout de build (10min max sur free tier)
- ✅ Déploiement très rapide (<1 minute total)
- ✅ Pas de ressources CPU/RAM gaspillées sur le build

---

## 🐛 Troubleshooting

### Erreur : "backend: not found"

**Cause** : L'exécutable n'a pas été copié dans le dossier.

**Solution** :
```bash
# Vérifier que le fichier existe
ls -la Backend-Docker-Graalvm/backend

# Si absent, le copier depuis backend/target/
Copy-Item backend/target/backend Backend-Docker-Graalvm/backend
```

---

### Erreur : "Permission denied"

**Cause** : L'exécutable n'a pas les permissions d'exécution.

**Solution** : Le Dockerfile s'en occupe automatiquement avec `chmod +x`. Si le problème persiste :
```bash
chmod +x Backend-Docker-Graalvm/backend
```

---

### L'exécutable ne démarre pas

**Vérifications** :
1. Profil Spring actif : `SPRING_PROFILES_ACTIVE=native,prod`
2. Supabase en Transaction Mode (port **6543**, pas 5432)
3. Variables d'environnement définies sur Render
4. Health check endpoint accessible : `/actuator/health`

---

### Build natif échoue en local

**Vérifications** :
1. GraalVM 21+ installé : `native-image --version`
2. Maven 3.8+ : `mvn -version`
3. Assez de mémoire : 8GB RAM minimum recommandé
4. Build avec logs : `./build-native.sh` affiche les erreurs

**Solution alternative** : Utiliser le JVM Optimisé (voir `DEPLOYMENT_OPTIONS.md`)

---

## 📚 Documentation Complémentaire

- **Guide complet Native Image** : `NATIVE_IMAGE_GUIDE.md`
- **Options de déploiement** : `DEPLOYMENT_OPTIONS.md`
- **Build natif détaillé** : `GRAALVM_NATIVE.md`
- **Render JVM optimisé** : `RENDER_DEPLOYMENT.md`

---

## 💡 Conseils

### Pour le développement

N'utilisez **PAS** l'exécutable natif en développement. Utilisez plutôt :
```bash
cd backend
mvn spring-boot:run
```

### Pour la production sur Render Free

L'exécutable natif est **parfait** car :
- Démarrage instantané après réveil
- Consommation mémoire minimale
- Coût : 0€

### Pour la production sur Render Paid

Le JVM optimisé peut suffire :
- Pas de mise en veille
- Build plus simple
- Performance acceptable
- Voir `RENDER_DEPLOYMENT.md`

---

## ✅ Checklist de Déploiement

- [ ] GraalVM installé localement
- [ ] Build natif réussi : `./build-native.bat`
- [ ] Exécutable copié : `Backend-Docker-Graalvm/backend` existe
- [ ] Exécutable committé et pushé
- [ ] Render configuré : Root Directory = `Backend-Docker-Graalvm`
- [ ] Variables d'environnement définies sur Render
- [ ] `SPRING_PROFILES_ACTIVE=native,prod`
- [ ] Supabase en Transaction Mode (port 6543)
- [ ] Premier déploiement testé
- [ ] Application démarre en <2s ✅
- [ ] Health check réussit ✅

---

## 🎉 Résultat Attendu

Après déploiement réussi :

```
✅ Build Render : ~30 secondes
✅ Démarrage app : <2 secondes
✅ Mémoire utilisée : 50-100 MB
✅ Premier visiteur : réponse immédiate
✅ Cold start : quasi inexistant
```

**Expérience utilisateur optimale sur Render Free Tier ! 🚀**
