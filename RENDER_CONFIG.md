# ⚙️ Configuration Render pour GraalVM Native Image

## 🚀 Vue d'ensemble

Ce Dockerfile compile l'exécutable natif **directement sur Render** avec GraalVM.

**Caractéristiques** :
- ⚡ Démarrage : <2 secondes
- 💾 Mémoire : 50-100 MB
- 📦 Image finale : ~150 MB
- ⏱️ Build initial : 5-10 minutes (puis cache)

---

## 📋 Configuration Render

### 1️⃣ Paramètres du Web Service

**Build & Deploy** :
```
Root Directory: Backend-Docker-Graalvm
Dockerfile Path: Dockerfile
Docker Command: (laisser vide)
```

### 2️⃣ Variables d'Environnement

**Obligatoires** :
```env
SPRING_PROFILES_ACTIVE=native,prod
PORT=8080
```

**Supabase (Transaction Mode - IMPORTANT)** :
```env
SPRING_DATASOURCE_URL=jdbc:postgresql://[HOST].supabase.co:6543/postgres?prepareThreshold=0
SPRING_DATASOURCE_USERNAME=postgres.[PROJECT_REF]
SPRING_DATASOURCE_PASSWORD=[YOUR_PASSWORD]
```

⚠️ **IMPORTANT** : Port **6543** (Transaction Mode), pas 5432 !

**JWT** :
```env
JWT_SECRET=[YOUR_SECRET_MIN_32_CHARS]
JWT_EXPIRATION=86400000
```

**Optionnel - HikariCP** :
```env
SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=5
SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=2
```

### 3️⃣ Health Check

```
Health Check Path: /actuator/health
```

### 4️⃣ Instance Type

**Render Free Tier** : Fonctionne parfaitement ✅
- Build : ~10 minutes (première fois, puis cache)
- Mémoire : 512 MB suffisent largement
- CPU : Aucun problème

**Render Starter** : Overkill mais encore mieux
- Build : ~5-7 minutes
- Pas de mise en veille

---

## ⏱️ Temps de Build

### Premier Build
```
Stage 1 (GraalVM): ~8-10 minutes
  - Download dependencies: ~1 min
  - Native compilation: ~7-9 min
  
Stage 2 (Runtime): ~30 secondes
  - Install packages: ~20s
  - Copy executable: <1s
  - Setup user: <1s

Total: ~10 minutes
```

### Builds Suivants (avec cache)
```
Si seulement code modifié: ~8 minutes
Si dépendances modifiées: ~10 minutes
Si pom.xml modifié: ~10 minutes
```

Le cache Docker de Render accélère les builds suivants ! 🚀

---

## 🎯 Déploiement Étape par Étape

### 1. Créer le Web Service sur Render

1. Connectez votre repository GitHub/GitLab
2. Choisissez **Docker** comme environnement
3. Configurez :
   - **Name** : `ventila-backend-native` (ou votre choix)
   - **Region** : Choisissez la plus proche de vos utilisateurs
   - **Branch** : `main` (ou votre branche)
   - **Root Directory** : `Backend-Docker-Graalvm`
   - **Instance Type** : Free (ou Starter)

### 2. Configurer les Variables d'Environnement

Ajoutez toutes les variables listées ci-dessus dans la section "Environment".

### 3. Premier Déploiement

1. Cliquez sur **"Create Web Service"**
2. Render va démarrer le build (~10 minutes)
3. Surveillez les logs :
   - ✅ Voir "Building native image..." → Normal
   - ✅ Voir "GraalVM Native Image" → Bon signe
   - ✅ Voir "Successfully compiled" → Excellent !
4. Une fois déployé, vérifiez :
   - Health check : `https://[votre-url]/actuator/health`
   - API : `https://[votre-url]/api/products`

### 4. Déploiements Suivants

À chaque `git push` :
- Render rebuilde automatiquement
- Build : ~8-10 minutes (avec cache)
- Démarrage : <2 secondes

---

## 🐛 Troubleshooting

### Build timeout (>10 minutes)

**Sur Free Tier** : Render peut limiter les ressources.

**Solutions** :
1. Ajouter `-DskipTests` (déjà fait)
2. Passer à Starter tier temporairement pour le build initial
3. Réessayer (parfois les serveurs sont chargés)

### Erreur : "Out of memory during build"

**Cause** : Compilation native consomme beaucoup de RAM.

**Solution** :
```dockerfile
# Déjà configuré dans le Dockerfile
RUN ./mvnw -Pnative native:compile -DskipTests \
    -Dorg.graalvm.buildtime.MemorySize=4G
```

Si le problème persiste, passez temporairement à Starter tier.

### Application crash au démarrage (exit code 128)

**Vérifications** :
1. Variables d'environnement définies ?
   - `SPRING_PROFILES_ACTIVE=native,prod`
   - Variables Supabase correctes ?
2. Port Supabase = **6543** (Transaction Mode) ?
3. `prepareThreshold=0` dans l'URL JDBC ?

**Logs à vérifier** :
```bash
# Sur Render, aller dans Logs et chercher :
- "Started BackendApplication in"
- Erreurs de connexion DB
- Stacktraces
```

### Health check échoue

**Vérifications** :
1. Spring Boot Actuator activé ?
   - `management.endpoints.web.exposure.include=health`
2. Path correct : `/actuator/health`
3. Application démarre bien ?

**Tester manuellement** :
```bash
curl https://[votre-url]/actuator/health
```

### Build échoue : "mvnw not found"

**Cause** : `.dockerignore` trop restrictif.

**Solution** : Vérifier `.dockerignore` :
```
!backend/
!backend/.mvn/
!backend/mvnw
```

---

## 📊 Monitoring

### Métriques à Surveiller

**Sur Render Dashboard** :
- **Memory** : Devrait rester <100 MB (vs 300-500 MB pour JVM)
- **CPU** : Pic au démarrage, puis stable
- **Response Time** : Doit être rapide (<100ms)

**Logs d'Application** :
```bash
# Démarrage réussi doit afficher :
Started BackendApplication in 1.234 seconds
```

### Performance Attendue

**Premier visiteur après cold start** :
```
Réveil container: ~5s (Render)
Démarrage app: <2s
Réponse: <1s
Total: ~8s
```

**Visiteurs suivants** :
```
Réponse: <100ms (app déjà chaude)
```

---

## 🔄 Workflow Recommandé

### Développement Local
```bash
# JVM standard pour itération rapide
cd backend
mvn spring-boot:run
```

### Test Native en Local (optionnel)
```bash
# Build natif local
./build-native.sh

# Test
./backend/target/backend
```

### Production sur Render
```bash
# 1. Commit vos changements
git add .
git commit -m "Update feature X"

# 2. Push vers la branche configurée sur Render
git push origin main

# 3. Render rebuilde automatiquement
# 4. Vérifier le déploiement dans Render Dashboard
```

---

## 💡 Optimisations Avancées

### Réduire le Temps de Build

**1. Utiliser le cache efficacement**
```dockerfile
# Déjà optimisé dans le Dockerfile :
# - pom.xml copié en premier
# - Dependencies téléchargées avant le code
# - Layers Docker bien structurés
```

**2. Build natif en parallèle**
```bash
# Déjà configuré dans pom.xml :
<buildArgs>-march=compatibility</buildArgs>
```

### Réduire la Taille de l'Image

**Image actuelle** : ~150 MB

**Pour réduire encore** :
- Utiliser `debian:bookworm-slim` ✅ (déjà fait)
- Upx compression (risqué, non recommandé)
- Alpine Linux (compatibilité limitée)

---

## 📚 Documentation Complémentaire

- **Guide complet Native Image** : `NATIVE_IMAGE_GUIDE.md`
- **Options de déploiement** : `DEPLOYMENT_OPTIONS.md`
- **Détails GraalVM** : `GRAALVM_NATIVE.md`

---

## ✅ Checklist de Déploiement

- [ ] Repository connecté à Render
- [ ] Root Directory = `Backend-Docker-Graalvm`
- [ ] Variables d'environnement configurées
- [ ] `SPRING_PROFILES_ACTIVE=native,prod`
- [ ] Supabase Transaction Mode (port 6543)
- [ ] `prepareThreshold=0` dans JDBC URL
- [ ] Health check path = `/actuator/health`
- [ ] Premier build lancé (~10 min)
- [ ] Application démarre en <2s ✅
- [ ] Health check passe ✅
- [ ] API fonctionne ✅

---

## 🎉 Résultat Attendu

Après déploiement réussi :

```
✅ Build : 8-10 minutes (première fois)
✅ Image : ~150 MB
✅ Démarrage : <2 secondes
✅ Mémoire : 50-100 MB
✅ Cold start : ~8 secondes total
✅ Performance : Excellente
```

**Prêt pour la production sur Render Free Tier ! 🚀**

---

## 🆘 Support

Si vous rencontrez des problèmes :

1. Vérifiez les logs Render en détail
2. Consultez `NATIVE_IMAGE_GUIDE.md` section Troubleshooting
3. Testez le build natif en local : `./build-native.sh`
4. Vérifiez la configuration Supabase : `SUPABASE_CONNECTION_FIX.md`

**Note** : Le premier build est toujours le plus long. Les suivants sont plus rapides grâce au cache Docker ! ⚡
