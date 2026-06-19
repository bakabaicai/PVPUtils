//--- Dos Barcos Flotando en el Mar Básico
//--- Por Jorge2017a3 + ayudaia 

uniform float time;
uniform vec2 resolution;
uniform vec2 mouse;

#define iTime time
#define iResolution resolution
#define iMouse vec4(mouse, 0.0, 0.0)

#define PI 3.14159265

// Funciones de unión de SDFs
float Sdf_U(float distA, float distB) { return min(distA, distB); }

// SDFs básicas para el barco
float sdCircle(vec2 p, float r) { return length(p) - r; }
float sdBox(vec2 p, vec2 b) {
    vec2 d = abs(p) - b;
    return length(max(d, 0.0)) + min(max(d.x, d.y), 0.0);
}

// --- LA MISMA FUNCIÓN DE MAR DEL EJEMPLO ANTERIOR ---
float calcularAlturaMar(float x) {
    float ola1 = 0.06 * sin(x * 3.0 + iTime * 1.5);
    float ola2 = 0.03 * sin(x * 7.0 - iTime * 2.5);
    float ola3 = 0.01 * cos(x * 15.0 + iTime * 4.0);
    return -0.4 + ola1 + ola2 + ola3;
}

// --- FUNCIÓN PARA DIBUJAR UN BARCO ---
// p: coordenadas uv de la pantalla
// xPos: posición fija en X donde queremos el barco
// colout: el color acumulado de la pantalla
// colBarco: el color que queremos para este barco
vec3 dibujarBarco(vec2 p, float xPos, vec3 colout, vec3 colBarco) {
    // 1. Encontrar la altura del mar justo en el centro del barco
    float yPos = calcularAlturaMar(xPos);
    
    // 2. Calcular la inclinación analizando dos puntos muy cercanos (derivada aproximada)
    float delta = 0.05;
    float yIzquierda = calcularAlturaMar(xPos - delta);
    float yDerecha   = calcularAlturaMar(xPos + delta);
    float angulo = atan(yDerecha - yIzquierda, delta * 2.0);
    
    // 3. Transformar el espacio local del barco (Traslación)
    // Lo subimos un poquito (+ 0.02) para que flote sobre la línea de flotación
    p -= vec2(xPos, yPos + 0.02);
    
    // 4. Rotar el espacio según el oleaje
    float c = cos(angulo); float s = sin(angulo);
    p = mat2(c, -s, s, c) * p;
    
    // 5. Modelar el barco con SDFs geométricas simples
    // Casco (Caja recortada o combinada)
    float cascoBase = sdBox(p + vec2(0.0, -0.01), vec2(0.22, 0.06));
    // Cortamos las esquinas del casco inclinando el espacio para que parezca lancha
    float proa = sdBox(p + vec2(-0.1, -0.05), vec2(0.12, 0.12));
    float popa = sdBox(p + vec2(0.1, -0.05), vec2(0.12, 0.12));
    
    // Mástil y Vela
    float mastil = sdBox(p - vec2(-0.02, 0.08), vec2(0.012, 0.12));
    float vela = sdBox(p - vec2(0.03, 0.18), vec2(0.04, 0.08));
    
    // Combinar formas
    float barcoSdf = Sdf_U(cascoBase, mastil);
    
    // Suavizado de bordes
    float blur = 0.005;
    
    // Pintar el cuerpo del barco y mástil
    colout = mix(colout, colBarco, smoothstep(blur, 0.0, barcoSdf));
    // Pintar la vela de color blanco
    colout = mix(colout, vec3(0.95), smoothstep(blur, 0.0, vela));
    
    return colout;
}

void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    // Sistema de coordenadas escalado x1.5
    vec2 uv = (2.0 * fragCoord - iResolution.xy) / iResolution.y;
    uv *= 1.5;
    
    // 1. CIELO Y SOL (FONDO)
    vec3 col = mix(vec3(0.9, 0.5, 0.3), vec3(0.2, 0.1, 0.3), uv.y * 0.5 + 0.5);
    float sunDist = length(uv - vec2(0.0, 0.4)) - 0.35;
    col = mix(col, vec3(0.95, 0.85, 0.4), smoothstep(0.02, 0.0, sunDist));
    
    // 2. DIBUJAR LOS 2 BARCOS (Capa Media)
    // Barco 1: A la izquierda (-0.6), de color rojo oscuro
    col = dibujarBarco(uv, -0.6, col, vec3(0.6, 0.15, 0.15));
    
    // Barco 2: A la derecha (0.5), un poco más pequeño/lejano si modificaras su escala, 
    // pero por ahora del mismo tamaño y color azul oscuro
    col = dibujarBarco(uv, 0.5, col, vec3(0.1, 0.15, 0.25));
    
    // 3. CAPA DE MAR DINÁMICO (FRENTE)
    // Se dibuja al final para que cubra la parte inferior de los cascos de los barcos
    float alturaAgua = calcularAlturaMar(uv.x);
    
    if(uv.y < alturaAgua) {
        vec3 colorAgua = vec3(0.1, 0.35, 0.5);
        // Degradado simple en el agua
        colorAgua = mix(colorAgua, vec3(0.05, 0.15, 0.3), (alturaAgua - uv.y) * 0.8);
        col = colorAgua;
    } else {
        // Espuma en la cresta
        if(uv.y - alturaAgua < 0.025) {
            col = mix(col, vec3(0.8, 0.95, 1.0), smoothstep(0.025, 0.0, uv.y - alturaAgua));
        }
    }
    
    fragColor = vec4(col, 1.0);
}

void main(void) {
    mainImage(gl_FragColor, gl_FragCoord.xy);
}
