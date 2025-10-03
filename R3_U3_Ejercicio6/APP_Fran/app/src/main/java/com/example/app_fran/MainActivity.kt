package com.example.app_fran

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PostresApp() }
    }
}

data class Receta(
    val dia: Int,
    val nombre: String,
    val resumen: String,
    val descripcion: String,
    @DrawableRes val imagenRes: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostresApp() {
    val pinkScheme = lightColorScheme(
        primary = Color(0xFFE91E63),
        onPrimary = Color.White,
        secondary = Color(0xFFF06292),
        surface = Color(0xFFFFF1F5),
        surfaceVariant = Color(0xFFFAD4E1),
        onSurfaceVariant = Color(0xFF4B2E3A)
    )

    MaterialTheme(colorScheme = pinkScheme) {
        Scaffold(
            topBar = { TopAppBar(title = { Text("30 días de postres") }) }
        ) { padding ->
            PostresScreen(Modifier.padding(padding).padding(16.dp))
        }
    }
}

@Composable
fun PostresScreen(modifier: Modifier = Modifier) {
    val recetas = remember { recetasPostres30() }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(recetas, key = { it.dia }) { receta ->
            RecetaCard(receta)
        }
    }
}

private suspend fun fadeTo(target: Float, setter: (Float) -> Unit, getter: () -> Float) {
    val steps = 12
    val start = getter()
    val delta = target - start
    if (delta == 0f) return
    repeat(steps) {
        setter(start + delta * (it + 1) / steps)
        delay(36L)
    }
}

private suspend fun growTo(target: Dp, setter: (Dp) -> Unit, getter: () -> Dp) {
    val steps = 10
    val start = getter()
    val delta = target.value - start.value
    if (delta == 0f) return
    repeat(steps) {
        setter(Dp(start.value + delta * (it + 1) / steps))
        delay(36L)
    }
}

@Composable
fun RecetaCard(receta: Receta) {
    val scope = rememberCoroutineScope()
    var expanded by rememberSaveable { mutableStateOf(false) }
    var overlayAlpha by remember { mutableFloatStateOf(0f) }
    var detailAlpha by remember { mutableFloatStateOf(0f) }
    var detailHeight by remember { mutableStateOf(0.dp) }

    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.fillMaxWidth()) {
            AssistChip(
                onClick = {},
                label = { Text("Día ${receta.dia}") },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.padding(start = 12.dp, top = 12.dp)
            )
            Text(
                receta.nombre,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                scope.launch { fadeTo(1f, { overlayAlpha = it }, { overlayAlpha }) }
                            },
                            onPress = {
                                scope.launch { fadeTo(1f, { overlayAlpha = it }, { overlayAlpha }) }
                                tryAwaitRelease()
                                scope.launch { fadeTo(0f, { overlayAlpha = it }, { overlayAlpha }) }
                            }
                        )
                    }
            ) {
                Image(
                    painter = painterResource(id = receta.imagenRes),
                    contentDescription = receta.nombre,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .alpha(overlayAlpha),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {}
                    Text(
                        text = receta.resumen,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
            Text(
                receta.resumen,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(12.dp)
            )
            TextButton(
                onClick = {
                    expanded = !expanded
                    scope.launch {
                        if (expanded) {
                            growTo(80.dp, { detailHeight = it }, { detailHeight })
                            fadeTo(1f, { detailAlpha = it }, { detailAlpha })
                        } else {
                            fadeTo(0f, { detailAlpha = it }, { detailAlpha })
                            growTo(0.dp, { detailHeight = it }, { detailHeight })
                        }
                    }
                },
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(if (expanded) "Ocultar detalles" else "Ver detalles")
            }
            if (detailHeight > 0.dp || detailAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(detailHeight)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .alpha(detailAlpha)
                ) {
                    Text(
                        receta.descripcion,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

fun recetasPostres30(): List<Receta> = listOf(
    Receta(1,"Torta Tres Leches","Bizcocho húmedo y dulce.",
        "Clásico latino con mezcla de tres leches; se sirve frío con canela.", R.drawable.postre1),
    Receta(2,"Tiramisú","Postre italiano de café.",
        "Capas de bizcotelas embebidas en espresso y crema de mascarpone con cacao.", R.drawable.postre2),
    Receta(3,"Cheesecake NY","Al horno, cremoso.",
        "Base de galletas con mantequilla y relleno suave de queso crema; horneado lento.", R.drawable.postre3),
    Receta(4,"Flan de Vainilla","Con caramelo suave.",
        "Cuajado a baño maría con vainilla natural y caramelo ámbar.", R.drawable.postre4),
    Receta(5,"Arroz con Leche","Tradicional.",
        "Arroz cocido en leche con canela y cáscara de limón; espolvorea canela.", R.drawable.postre5),
    Receta(6,"Mousse de Chocolate","Aireado y rico.",
        "Chocolate derretido con yemas y crema batida; enfriar hasta firme.", R.drawable.postre6),
    Receta(7,"Brownies","Húmedos y chocolatosos.",
        "Textura densa con bordes crujientes; se pueden agregar nueces.", R.drawable.postre7),
    Receta(8,"Crema Volteada","Similar al flan.",
        "Más yemas para textura sedosa; caramelo profundo al servir.", R.drawable.postre8),
    Receta(9,"Tres Leches de Fresa","Variante frutal.",
        "Bizcocho empapado con tres leches y cubierta de fresas frescas.", R.drawable.postre9),
    Receta(10,"Helado de Vainilla","Casero.",
        "Versión no-churn: crema batida, condensada y vainilla; congelar.", R.drawable.postre10),
    Receta(11,"Churros","Crujientes por fuera.",
        "Masa escaldada frita; azúcar y canela. Ideal con salsa de chocolate.", R.drawable.postre11),
    Receta(12,"Panna Cotta","Italiana.",
        "Crema con gelatina y vainilla; acompaña con coulis de frutas rojas.", R.drawable.postre12),
    Receta(13,"Pie de Limón","Ácido y dulce.",
        "Relleno cremoso de limón sobre base de galletas; merengue opcional.", R.drawable.postre13),
    Receta(14,"Galletas Chocochips","Clásicas.",
        "Mantequilla dorada, azúcar morena y chispas; centro suave.", R.drawable.postre14),
    Receta(15,"Crème Brûlée","Francés.",
        "Crema horneada en ramekins; carameliza azúcar hasta formar costra.", R.drawable.postre15),
    Receta(16,"Tarta de Manzana","Rústica.",
        "Láminas de manzana con canela en masa quebrada; hornea hasta dorar.", R.drawable.postre16),
    Receta(17,"Baklava","Capas dulces.",
        "Masa filo con mezcla de nueces y almíbar de miel o azahar.", R.drawable.postre17),
    Receta(18,"Volcán de Chocolate","Centro líquido.",
        "Bizcocho individual con corazón fundido; servir caliente.", R.drawable.postre18),
    Receta(19,"Banoffee Pie","Banana + toffee.",
        "Base de galleta, dulce de leche, rodajas de banana y crema.", R.drawable.postre19),
    Receta(20,"Suspiro Limeño","Peruano.",
        "Manjar blanco espeso coronado con merengue de oporto y canela.", R.drawable.postre20),
    Receta(21,"Pie de Maracuyá","Aromático.",
        "Ácido y perfumado; corona con merengue italiano o crema.", R.drawable.postre21),
    Receta(22,"Alfajores","Mantecosos.",
        "Tapas de maicena rellenas de manjar; bordes con coco.", R.drawable.postre22),
    Receta(23,"Quesillo Venezolano","Tipo flan.",
        "Licuar todo y hornear a baño maría; textura firme.", R.drawable.postre23),
    Receta(24,"Paletas de Fruta","Refrescantes.",
        "Licuar fruta y congelar en moldes; perfectas para verano.", R.drawable.postre24),
    Receta(25,"Cheesecake Frío","Sin horno.",
        "Relleno con gelatina y crema sobre base de galletas; refrigerar.", R.drawable.postre25),
    Receta(26,"Mazamorra Morada","Peruana.",
        "Maíz morado con frutas y especias; espesa con chuño.", R.drawable.postre26),
    Receta(27,"Tarta de Queso Vasca","Cremosa.",
        "Horneado a alta temperatura para superficie caramelizada.", R.drawable.postre27),
    Receta(28,"Profiteroles","Rellenos.",
        "Masa choux con crema pastelera y baño de chocolate.", R.drawable.postre28),
    Receta(29,"Rollos de Canela","Esponjosos.",
        "Masa levada con canela; glaseado de queso crema.", R.drawable.postre29),
    Receta(30,"Tarta de Frutas","Colorida.",
        "Crema pastelera y frutas frescas brilladas sobre base crujiente.", R.drawable.postre30),
)
