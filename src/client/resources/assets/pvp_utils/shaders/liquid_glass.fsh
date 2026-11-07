#version 330 core

uniform sampler2D Background;

#define MAX_SHAPES 16

uniform vec4 Meta;
uniform vec4 Global0;
uniform vec4 Global1;
uniform vec4 Global2;
uniform vec4 ShadowOff;
uniform vec4 ShadowColor;
uniform vec4 Rects[MAX_SHAPES];
uniform vec4 Rads[MAX_SHAPES];
uniform vec4 Optics[MAX_SHAPES];
uniform vec4 Tints[MAX_SHAPES];

layout(location = 0) out vec4 fragColor;

vec3 roundRectSDF(vec2 p, vec2 size, vec4 radius) {
    vec2 halfSize = size * 0.5;
    vec2 q0 = p;
    vec2 s = step(0.0, q0);
    float r = mix(mix(radius.x, radius.w, s.y), mix(radius.y, radius.z, s.y), s.x);
    vec2 q = abs(q0) - halfSize + r;
    float dist = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
    vec2 m = max(q, 0.0);
    float l = length(m);
    vec2 n;
    if (l > 1e-6) {
        n = m / l;
    } else {
        n = (q.x > q.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0);
    }
    n *= sign(q0);
    return vec3(dist, n);
}

struct SDFResult { float dist; vec2 normal; int index; };

SDFResult opSmoothUnion(SDFResult a, SDFResult b, float k) {
    if (k <= 0.0) {
        if (a.dist < b.dist) return a;
        return b;
    }
    float h = clamp(0.5 + 0.5 * (a.dist - b.dist) / k, 0.0, 1.0);
    float d = mix(a.dist, b.dist, h) - k * h * (1.0 - h);
    vec2 n = normalize(mix(a.normal, b.normal, h));
    int index = (a.dist < b.dist) ? a.index : b.index;
    return SDFResult(d, n, index);
}

SDFResult field(vec2 p) {
    int n = int(Meta.z + 0.5);
    SDFResult res = SDFResult(1e6, vec2(0.0), -1);
    bool first = true;
    for (int i = 0; i < MAX_SHAPES; i++) {
        if (i >= n) break;
        vec2 minP = Rects[i].xy;
        vec2 size = Rects[i].zw;
        vec2 center = minP + size * 0.5;
        vec3 sdf = roundRectSDF(p - center, size, Rads[i]);
        SDFResult s = SDFResult(sdf.x, sdf.yz, i);
        if (first) { res = s; first = false; }
        else { res = opSmoothUnion(res, s, Optics[i].x); }
    }
    return res;
}

vec3 blurSample(vec2 uv, float radiusPx) {
    if (radiusPx < 0.5) return texture(Background, uv).rgb;
    vec2 radius = radiusPx / Meta.xy;
    vec4 color = texture(Background, uv);
    float angStep = 6.2831853 / 16.0;
    for (float d = 0.0; d < 6.2831853; d += angStep) {
        for (float i = 0.2; i <= 1.0; i += 0.2) {
            color += texture(Background, uv + vec2(cos(d), sin(d)) * radius * i);
        }
    }
    return (color / 81.0).rgb;
}

void main() {
    vec2 inSize = Meta.xy;
    vec2 coord = gl_FragCoord.xy;
    vec2 uv = coord / inSize;

    vec3 bg = texture(Background, uv).rgb;
    vec3 base = bg;

    float shadowFactor = Global1.w;
    if (shadowFactor > 0.001) {
        float sd = field(coord + ShadowOff.xy).dist;
        float sh = shadowFactor * exp(-max(0.0, sd) / max(Global1.z, 1e-3));
        base = mix(base, ShadowColor.rgb, clamp(ShadowColor.a * sh, 0.0, 1.0));
    }

    SDFResult f = field(coord);
    float dist = f.dist;
    float delta = fwidth(dist) + 1e-4;
    float alpha = 1.0 - smoothstep(-delta, delta, dist);
    if (alpha < 0.001) { fragColor = vec4(base, 1.0); return; }

    int idx = f.index;
    if (idx < 0) { fragColor = vec4(base, 1.0); return; }

    vec4 tint = Tints[idx];
    vec2 normal = f.normal;
    float nlen = length(normal);
    if (nlen > 1e-6) normal /= nlen;

    float nmerged = -dist;
    float thickness = max(Global2.x, 1.0);
    float xR = clamp(1.0 - nmerged / thickness, 0.0, 1.0);
    float thetaI = asin(pow(xR, 2.0));
    float thetaT = asin(sin(thetaI) / max(Global0.x, 1.001));
    float edgeFactor = -tan(thetaT - thetaI);
    if (nmerged >= thickness) edgeFactor = 0.0;

    vec2 refrOffset = -normal * edgeFactor * 0.08 * vec2(inSize.y / inSize.x, 1.0);

    vec3 blurred = blurSample(uv + refrOffset, Global0.w);
    float disp = Global0.y;
    vec3 dispPixel;
    dispPixel.r = blurSample(uv + refrOffset * (1.0 - (0.985 - 1.0) * disp), Global0.w).r;
    dispPixel.g = blurred.g;
    dispPixel.b = blurSample(uv + refrOffset * (1.0 - (1.015 - 1.0) * disp), Global0.w).b;

        vec3 glass = mix(dispPixel, tint.rgb, tint.a * 0.5);

    float edgeProx = exp(-abs(dist) / 9.0);
    float fres = clamp(pow(1.0 + dist / 1500.0, 5.0), 0.0, 1.0);
    glass = mix(glass, tint.rgb, fres * Global0.z * edgeProx);
    glass += tint.rgb * Global1.y * edgeProx * 0.5;

    float opacity = Optics[idx].w;
        vec3 finalColor = mix(base, glass, Global1.x * opacity);

    fragColor = vec4(finalColor, alpha * opacity);
}
