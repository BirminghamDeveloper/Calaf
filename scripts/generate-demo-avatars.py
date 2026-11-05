# Run: python3 scripts/generate-demo-avatars.py (needs Pillow). Regenerates the demo avatars.
# Generates abstract, faceless illustrated avatars. Not based on any real person.
from PIL import Image, ImageDraw
import math, os
OUT=os.path.join(os.path.dirname(os.path.abspath(__file__)),'..','app','src','demo','res','drawable-nodpi')
S=384; K=2; W=S*K
palettes=[
 # bg top, bg bottom, garment, skin, hair/scarf, accent
 ('#F6D5C3','#E8A87C','#2F6F5E','#8D5A3B','#2B2118','#F2C14E'),
 ('#CFE8E3','#7FB7B0','#7A3E65','#B97A56','#4A2C4F','#F4D35E'),
 ('#FBE3A6','#F2A65A','#264653','#6F4328','#14110F','#E76F51'),
 ('#D9D2F2','#8E7CC3','#E9C46A','#A9683F','#2D1E2F','#FFFFFF'),
 ('#CDE7F0','#5FA8D3','#BC4B51','#C68863','#33261D','#FFE8D6'),
 ('#F2D6E4','#D88AB0','#1D3557','#7B4B2E','#3A2A20','#A8DADC'),
 ('#DDEBC3','#8CB369','#5E3A87','#9C6644','#1B1B1E','#F4F1BB'),
 ('#FFE0C2','#F4A261','#3D5A80','#B07A57','#2A2118','#EE6C4D'),
 ('#C9E4DE','#6BA292','#8C2F39','#5E3A26','#120F0D','#F7D6E0'),
 ('#E6D3F0','#A178C9','#2A6F97','#C28E6B','#3B2F2F','#FAE0A8'),
]
def grad(draw,top,bottom):
    t=tuple(int(top[i:i+2],16) for i in (1,3,5)); b=tuple(int(bottom[i:i+2],16) for i in (1,3,5))
    for y in range(W):
        f=y/(W-1); draw.line([(0,y),(W,y)],fill=tuple(int(t[j]+(b[j]-t[j])*f) for j in range(3)))
for n,(bt,bb,garment,skin,hair,accent) in enumerate(palettes,1):
    img=Image.new('RGB',(W,W)); d=ImageDraw.Draw(img); grad(d,bt,bb)
    # soft decorative circles, different for each avatar
    for k in range(3):
        a=(n*37+k*91)%360; cx=W*(0.2+0.6*((math.sin(math.radians(a))+1)/2)); cy=W*(0.15+0.3*k/2)
        r=W*(0.12+0.05*k); d.ellipse([cx-r,cy-r,cx+r,cy+r],outline=accent,width=K*3)
    cx=W//2
    # shoulders
    d.pieslice([cx-W*0.46,W*0.70,cx+W*0.46,W*1.35],180,360,fill=garment)
    # neck
    d.rounded_rectangle([cx-W*0.07,W*0.52,cx+W*0.07,W*0.74],radius=W*0.03,fill=skin)
    style=n%4
    hy=W*0.40; hr=W*0.19
    if style in (0,2):   # headscarf: wide rounded shape around the face
        d.ellipse([cx-hr*1.45,hy-hr*1.35,cx+hr*1.45,hy+hr*1.75],fill=hair)
        d.pieslice([cx-W*0.30,W*0.62,cx+W*0.30,W*0.98],180,360,fill=hair)
        d.ellipse([cx-hr*0.95,hy-hr*0.85,cx+hr*0.95,hy+hr*1.05],fill=skin)
    elif style==1:       # long hair behind
        d.rounded_rectangle([cx-hr*1.25,hy-hr*0.2,cx+hr*1.25,hy+hr*2.2],radius=W*0.09,fill=hair)
        d.ellipse([cx-hr,hy-hr,cx+hr,hy+hr*1.15],fill=skin)
        d.pieslice([cx-hr*1.15,hy-hr*1.3,cx+hr*1.15,hy+hr*0.5],180,360,fill=hair)
    else:                # short hair and a kufi style cap
        d.ellipse([cx-hr,hy-hr,cx+hr,hy+hr*1.15],fill=skin)
        d.pieslice([cx-hr*1.1,hy-hr*1.25,cx+hr*1.1,hy+hr*0.55],180,360,fill=accent if n%3 else hair)
    img=img.resize((S,S),Image.LANCZOS).convert('P',palette=Image.ADAPTIVE,colors=128)
    img.save(os.path.join(OUT,f'avatar_{n:02d}.png'),optimize=True)
print('ok')
