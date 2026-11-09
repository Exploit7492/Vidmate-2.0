import base64,re,sys,os
KEY="android.view"
def dec(s):
    try:
        raw=base64.b64decode(s+"="*(-len(s)%4))
    except Exception:
        return None
    out=bytes(b^ord(KEY[i%len(KEY)]) for i,b in enumerate(raw))
    try: return out.decode('utf-8')
    except: return None
if __name__=="__main__":
    pat=re.compile(r'aaaQ\.a(?:a)?\("([^"]+)"\)')
    root=sys.argv[1] if len(sys.argv)>1 else "decompiled/plugin/sources"
    seen={}
    for dp,_,fs in os.walk(root):
        for fn in fs:
            if not fn.endswith('.java'): continue
            p=os.path.join(dp,fn)
            try: t=open(p,encoding='utf-8',errors='replace').read()
            except: continue
            for m in pat.finditer(t):
                d=dec(m.group(1))
                if d is not None:
                    seen.setdefault(d,0)
                    seen[d]+=1
    print("distinct decoded strings:",len(seen))
    for k in sorted(seen):
        print(f"{seen[k]:5}  {k}")
