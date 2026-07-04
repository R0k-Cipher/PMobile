package com.example.projectmobile

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.projectmobile.config.ThresholdConfig
import com.example.projectmobile.data.DatabaseHelper
import com.example.projectmobile.model.HistoryItem
import com.example.projectmobile.security.SessionManager
import com.example.projectmobile.ui.theme.ProjectMobileTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        setContent {
            ProjectMobileTheme {
                LeanMassApp()
            }
        }
    }
}

@Composable
fun LeanMassApp() {
    val context = LocalContext.current
    val db = remember { DatabaseHelper(context) }
    val session = remember { SessionManager(context) }

    var screen by remember {
        mutableStateOf(
            if (session.isLoggedIn()) "calculator" else "login"
        )
    }

    when (screen) {
        "login" -> LoginScreen(
            db = db,
            session = session,
            onLoginSuccess = { screen = "calculator" },
            onGoRegister = { screen = "register" }
        )

        "register" -> RegisterScreen(
            db = db,
            onRegisterSuccess = { screen = "login" },
            onBackLogin = { screen = "login" }
        )

        "calculator" -> CalculatorScreen(
            db = db,
            session = session,
            onHistory = { screen = "history" },
            onLogout = {
                session.logout()
                screen = "login"
            }
        )

        "history" -> HistoryScreen(
            db = db,
            session = session,
            onBack = { screen = "calculator" }
        )
    }
}

@Composable
fun LoginScreen(
    db: DatabaseHelper,
    session: SessionManager,
    onLoginSuccess: () -> Unit,
    onGoRegister: () -> Unit
) {
    val context = LocalContext.current
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AuthContainer(title = "Connexion") {
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Nom d'utilisateur") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Mot de passe") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                if (username.isBlank() || password.isBlank()) {
                    Toast.makeText(context, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                val result = db.loginUser(username.trim(), password)

                if (result.first) {
                    session.saveUser(result.second, username.trim())
                    onLoginSuccess()
                } else {
                    Toast.makeText(context, "Identifiants incorrects", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Se connecter")
        }

        TextButton(onClick = onGoRegister) {
            Text("Créer un compte")
        }
    }
}

@Composable
fun RegisterScreen(
    db: DatabaseHelper,
    onRegisterSuccess: () -> Unit,
    onBackLogin: () -> Unit
) {
    val context = LocalContext.current
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    AuthContainer(title = "Inscription") {
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Nom d'utilisateur") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Mot de passe") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = confirm,
            onValueChange = { confirm = it },
            label = { Text("Confirmer le mot de passe") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                if (username.isBlank() || password.isBlank() || confirm.isBlank()) {
                    Toast.makeText(context, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (password.length < 6) {
                    Toast.makeText(context, "Le mot de passe doit contenir au moins 6 caractères", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (password != confirm) {
                    Toast.makeText(context, "Les mots de passe ne correspondent pas", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                val success = db.registerUser(username.trim(), password)

                if (success) {
                    Toast.makeText(context, "Compte créé avec succès", Toast.LENGTH_SHORT).show()
                    onRegisterSuccess()
                } else {
                    Toast.makeText(context, "Utilisateur déjà existant", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Créer le compte")
        }

        TextButton(onClick = onBackLogin) {
            Text("Retour à la connexion")
        }
    }
}

@Composable
fun CalculatorScreen(
    db: DatabaseHelper,
    session: SessionManager,
    onHistory: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current

    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Homme") }
    var resultText by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            AppHeader(
                title = "LeanMass Calculator",
                actionText = "Déconnexion",
                onAction = onLogout
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(20.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Utilisateur : ${session.getUsername()}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it },
                label = { Text("Poids en kg") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = height,
                onValueChange = { height = it },
                label = { Text("Taille en cm") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = gender == "Homme",
                    onClick = { gender = "Homme" }
                )
                Text("Homme")

                Spacer(Modifier.width(20.dp))

                RadioButton(
                    selected = gender == "Femme",
                    onClick = { gender = "Femme" }
                )
                Text("Femme")
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    val w = weight.toDoubleOrNull()
                    val h = height.toDoubleOrNull()

                    if (w == null || h == null || w <= 0 || h <= 0) {
                        Toast.makeText(context, "Veuillez saisir des valeurs valides", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val lbm = if (gender == "Homme") {
                        (0.407 * w) + (0.267 * h) - 19.2
                    } else {
                        (0.252 * w) + (0.473 * h) - 48.3
                    }

                    val isGood = if (gender == "Homme") {
                        lbm >= ThresholdConfig.MALE_MIN_LBM
                    } else {
                        lbm >= ThresholdConfig.FEMALE_MIN_LBM
                    }

                    val status = if (isGood) {
                        "Résultat satisfaisant"
                    } else {
                        "Résultat à surveiller"
                    }

                    resultText = "LBM = %.2f kg".format(lbm)
                    statusText = status

                    db.addHistory(
                        userId = session.getUserId(),
                        weight = w,
                        height = h,
                        gender = gender,
                        lbm = lbm,
                        status = status
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Calculer")
            }

            Spacer(Modifier.height(20.dp))

            if (resultText.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(resultText, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        Text(statusText, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            OutlinedButton(
                onClick = onHistory,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Voir historique")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    db: DatabaseHelper,
    session: SessionManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var history by remember {
        mutableStateOf(db.getHistory(session.getUserId()))
    }

    Scaffold(
        topBar = {
            AppHeader(
                title = "Historique",
                actionText = "Retour",
                onAction = onBack
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Button(
                onClick = {
                    db.deleteAllHistory(session.getUserId())
                    history = db.getHistory(session.getUserId())
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Supprimer tout l'historique")
            }

            Spacer(Modifier.height(16.dp))

            if (history.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Aucun calcul enregistré")
                }
            } else {
                LazyColumn {
                    items(history) { item ->
                        HistoryCard(
                            item = item,
                            modifier = Modifier.combinedClickable(
                                onClick = {},
                                onLongClick = {
                                    db.deleteHistoryItem(item.id)
                                    history = db.getHistory(session.getUserId())
                                    Toast.makeText(context, "Calcul supprimé", Toast.LENGTH_SHORT).show()
                                }
                            )
                        )

                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryCard(item: HistoryItem, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Date : ${item.createdAt}")
            Text("Sexe : ${item.gender}")
            Text("Poids : ${item.weight} kg")
            Text("Taille : ${item.height} cm")
            Text("LBM : %.2f kg".format(item.lbm))
            Text("Statut : ${item.status}")
            Text("Appui long pour supprimer")
        }
    }
}

@Composable
fun AuthContainer(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(title, style = MaterialTheme.typography.headlineMedium)

                Spacer(Modifier.height(24.dp))

                content()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppHeader(
    title: String,
    actionText: String,
    onAction: () -> Unit
) {
    TopAppBar(
        title = { Text(title) },
        actions = {
            TextButton(onClick = onAction) {
                Text(actionText)
            }
        }
    )
}