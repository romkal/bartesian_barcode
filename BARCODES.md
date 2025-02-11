# About Bartesian barcodes

## Barcode structure

Bartesian barcodes are based on [Code128](https://en.wikipedia.org/wiki/Code_128) format, although they don't conform to the standard.

What is the same is:
* Bar/space widths conform to Code128 standard having thickness between 1 - 4 units
* Each symbol consists of 6 symbols (3 bars and 3 spaces)
* Each symbol has total thickness of 11
* Symbols are decoded according to Code128 C standard and encode numbers between 0 and 99
* The last symbol is a checksum calculated according to Code128

What is different:
* There is no start symbol as the orientation of the code is always the same and you cannot put a pod backwards
* The stop symbol is just a single bar with thickness 2
* The barcode is always 68 units in width: 5 data symbols (thickness 55) + 1 checksum symbol (thickness 11) + the last bar of 2.

The code for a pod is created by concatenating all data symbols decoded as Code128 C double-digit numbers, so out of 5 data symbols, each a number between 0 and 99 we get a number in the range of 0 to 9,999,999,999.

## Code interpretation

The code is interpreted as a binary number and represented as a series of bits, where each bit or a set of bits (interpreted as a binary number) represents a different information. The encoding is as follows:

| Bits | Meaning |
|------|---------|
| 0    | Always set to 1 |
| 1 - 2  | 0 = Low ball glass, 1 = High ball glass, 2 = Shaker |
| 3 - 7 | Amount of water (value between 0 and 31) |
| 8 | Drink with Gin |
| 9 | Drink with Rum |
| 10 | Drink with Vodka |
| 11 | Drink with Tequilla |
| 12 | Drink with Whiskey |
| 13 - 15 | Amount of the first selected alcohol (in the order above). Value between 0 - 7. |
| 16 - 18 | Amount of the second selected alcohol (in the order above). Value between 0 - 7. |
| 19 - 21 | Amount of the third selected alcohol (in the order above). Value between 0 - 7. |
| 22 - 30 | Drink ID. Value between 0 - 511. |

A list of drinks that I know about and a breakdown of their code available in [a spreadsheet](https://docs.google.com/spreadsheets/d/1Q9W_FA9sHv2oQAjTC1ddjxK-sF1B4wvLVJ0a20RjEbw/edit?usp=sharing).

As seen above, a barcode can encode a separate amount of up to 3 different kinds of alcohol, although when the barcode chooses 4 kind of alcohol, it seems that the amount of all of them is encoded at bits 13-15 and bits at 16-21 are ignored.

### About Drink ID

I was not able to find any change of behavior of the machine based on the content of bits 22-30.

There are a couple of drinks that have those bits set to 0, but other than that they are all unique per a type of coctail and they seem to stay consistent for all pods of a given kind.

My theory, that it is simply a Drink ID and not some other information is based on the fact that a bunch of drinks in the Advent Calendar of 2024, even when very different, had oddly consequtive numbers, so it almost seems like a recipe number that is assigned consequtively.

What is interesting as well, is that I was able to find "Rum Breeze" pods that had both Drink ID set to 0 and 14 (relatively low number) so it seems that only at some point Bartesian started to assign those Drink ID numbers, but as they don't change the behavior of the machine anyway, it doesn't really matter.

### About alcohol amount

When the alcohol bit (8-11) is off for a given alcohol, it will not be dispensed at all. When the alcohol bit is on, it will be dispensed according to the value encoded in respective set of bits between 13 and 21.

Alcohols are always dispensed in the order they are encoded in the bardode, that is Gin/Rum, Vodka, Tequilla and Whiskey. For example if bits for Whiskey and Tequilla are on, the amount of Whiskey will be encoded on bits 13-15 and amount for Tequilla will be on bits 16-18.

Even if the alcohol amount is encoded as 0, it will still be dispensed, if it is selected on bits 8 to 12.

According to my testing, the amount of alcohol dispensed based on the amount encoded on respective bits looks as follows:

![Alco amount](https://docs.google.com/spreadsheets/d/e/2PACX-1vRodSRUaVwdZWtSMMX9A1C2gBj7r3dJI0NP5Gc46JSE01ab9HSUEPO5t1H615Til952f0e60So0YkoZ/pubchart?oid=666733269&format=image)

The graph looks almost linear, but despite testing multiple times the values were staying like this and not getting any closer to a pure linear trend.

### About water amount

It seems, that even when amount of water is set at 0, the machine still dispenses about 0.2oz of water anyway, so even for barcodes encoding no alcohol and 0 water, you will get 0.2oz of liquid dispensed.

The graph showing amount of water dispensed based on the value encoded is as follows:

![Water amount](https://docs.google.com/spreadsheets/d/e/2PACX-1vRodSRUaVwdZWtSMMX9A1C2gBj7r3dJI0NP5Gc46JSE01ab9HSUEPO5t1H615Til952f0e60So0YkoZ/pubchart?oid=1827687697&format=image)

It is really weird that for value 2 there is more water dispensed than for 3, but.. it is what it is. It is interesting that the trend is not linear but seems to change the gradient few times.

## How the machine works

The machine has a single pump and a bunch of solenoids that open/close respective lines causing this line to be activated and pulled by the pump.

Each cycle works like this:

1. Lines for respective alcohols are activated, one by one, in the order of Gin/Rum, Vodka, Tequilla and Whiskey depending if they are selected by bits 8-12 and are active for the duration encoded in bits 13-21,
2. Line for water is activated for the duration encoded on bits 3-7
3. Line for air is activated exactly for 10 seconds to flush the system of liquids that would stay in pipes and get into next drink.

It seems that the "strength" setting on the machine has always the following effect:
* _Light_ - dispenses 70% of alcohol that is dispensed in the standard setting
* _Strong_ - dispenses 170% of alcohol that is dispensed in the standard setting
* _Mocktail_ - adds extra water in the amount of 70% of alcohol that would be dispensed in the standard setting.

## Metodology

The testing was performed on my, single machine, so I cannot tell if all machines behave the same way.

> [!NOTE]
> The surprisingly large amount of alcohol dispensed in comparison to what is written on a box is still bothering me and I still suspect that my machine might be missbihaving, although one more person was able to confirm my observations.

To check the amount of liquids dispensed I'd run the machine through an empty pod (later using a custom barcodes I printed) and weighted the amount of liquid dispensed after few cycles. The water tank was filled to the max line to be able to account for water loss. I'd then refill the water tank exactly to the line it was before and weight my liquid again to check how much alcohol was dispensed. See my sample video [here](https://youtu.be/oueRorEYBjE).

For some barcodes I just weighted the amount of liquid, as I knew that the barcode already encodes 0 for alcohol or water and I can account fot that.

To test all different combinations and how much they pour I printed different barcode configurations I wanted to test. One of those sheets with 85 barcodes [here](https://drive.google.com/file/d/1mV0Sse8WH4wEWm_hdy49QXIDL6aOf4BS/view?usp=sharing). For each of them I'd weight the liquid dispensed. In some cases for different machine settings (light, strong, mocktail).
