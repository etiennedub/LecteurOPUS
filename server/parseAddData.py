import csv
import xml.etree.ElementTree as ET

with open('password.txt', 'r') as f:
    password = f.read().splitlines()

password = [i.strip() for i in password]
print(password)

tree = ET.parse('xml/operators.xml')
root = tree.getroot()

opNames = []
for child in root:
    opNames.append(child.attrib['name'])


bus = {}
with open('addData.csv', 'r') as csvfile:
    spamreader = csv.reader(csvfile, delimiter=';', quotechar='|')
    for row in spamreader:
        row = [i.strip() for i in row]
        id, time, busId, operatorId, in1, in2, add = row

        # Some data are invert
        if in1 in opNames:
            busName = in2
            operatorName = in1
        elif in2 in opNames:
            busName = in1
            operatorName = in2
        else:
            continue

        entry = (id, time, busId, operatorId, add)

        op = bus.get(operatorName)
        if(op):
            b = op.get(busName)
            if(b):
                bus[operatorName][busName].append(entry)
            else:
                bus[operatorName][busName] = [entry]
        else:
            bus[operatorName] = {busName : [entry]}


tree = ET.parse('xml/operators.xml')
root = tree.getroot()

for child in root:
    opIdXml = child.attrib['id']
    opName = child.attrib['name']
    fileName = child.attrib['file']
    if(fileName and bus.get(opName)):
        tree = ET.parse('xml/{}.xml'.format(fileName))
        opRoot = tree.getroot()
        for opChild in opRoot:
            opChild.set('id', "")
            result = {}
            maxVal = 0
            selectedValue = None

            busName = opChild.attrib['name']
            try:
                entries = bus.get(opName).get(busName)
            except NoneType:
                continue
            if(not entries):
                continue

            for entry in entries:
                userId, time, busId, opIdEntry, add = entry;
                add = 1 if add == 'true' else -2
                if userId in password:
                    add = 100000;
                userId += time

                if(opIdEntry != opIdXml):
                    continue

                op = result.get(opIdEntry)
                if(op):
                    b = op.get(busId)
                    if(b):
                        if(userId not in b[1]):
                            result[opIdEntry][busId][0] += add
                            result[opIdEntry][busId][1].append(userId)
                            add = result[opIdEntry][busId][0]
                    else:
                        result[opIdEntry][busId] = [add, [userId]]
                else:
                    result[opIdEntry] = {busId : [add, [userId]]}

                if(maxVal < add):
                    selectedValue = int(busId)
                    maxVal = add

            if(selectedValue and maxVal > 1):
                print(opName, busName, maxVal)
                opChild.set('id', str(selectedValue))

        tree.write('xml/{}.xml'.format(fileName))



