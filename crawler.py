import requests
import json


class Data:
    def __init__(self,
                 slot,
                 validators_cnt,
                 messages_cnt,
                 beaconchain_cnt):
        self.slot = slot
        self.validators_cnt = validators_cnt
        self.messages_cnt = messages_cnt
        self.beaconchain_cnt = beaconchain_cnt

    def __str__(self):
        return f'slot: {self.slot}, ' \
               f'messages: {self.messages_cnt}, ' \
               f'validators: {self.validators_cnt}, ' \
               f'bc: {self.beaconchain_cnt}'


first_slot = 4600500
last_slot = 4601000
url_validators = "http://localhost:5051/eth/v1/crawler/validators/"
url_messages = "http://localhost:5051/eth/v1/crawler/messages/"
url_beaconchain = "https://beaconcha.in/api/v1/block/"

data = []
for i in range(first_slot, last_slot):
    response = requests.get(url_validators + str(i))
    if response.ok:
        validators_json = json.loads(response.content)
        validators_cnt = int(validators_json['data']['count'])
        response = requests.get(url_messages + str(i))
        messages_json = json.loads(response.content)
        messages_cnt = len(messages_json['data'])
        response = requests.get(url_beaconchain + str(i))
        beaconchain_json = json.loads(response.content)
        beaconchain_cnt = int(beaconchain_json['data']['syncaggregate_participation'] * 512)

        d = Data(i, validators_cnt, messages_cnt, beaconchain_cnt)
        data.append(d)
        print(d)

        if d.validators_cnt > d.beaconchain_cnt:
            print(f'Fetched more validators for slot {i}, difference: {d.validators_cnt - d.beaconchain_cnt}')


