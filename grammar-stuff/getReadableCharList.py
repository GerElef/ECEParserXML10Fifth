lines = []
with open('charList.txt', 'r') as f:
    lines = f.read()

lines = lines.split('\n')

with open("res.txt", "w",encoding="utf-8") as f:
    for line in lines:
        finalstr = ""
        if line == "":
            continue
        subline = line.split('-')
        if len(subline) > 1:
            strs = []
            for sub in subline:
                finalstr += "-" + chr(int(sub.replace("#x", ""),16))
            finalstr = finalstr[1:len(finalstr)]
        else:
            finalstr = chr(int(line.replace("#x", ""),16))

        f.write(finalstr + "\n")
    

    


