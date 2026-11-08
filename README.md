# External Sorting Algorithms

This JavaFX application sorts text files and animates the operations performed by three external sorting algorithms.

- **Polyphase Merge Sort**
- **Balanced Merge Sort**
- **External Radix Sort**

## Table of Contents
- [Overview](#overview)
- [Installation](#installation)
- [Usage](#usage)
- [Sorting Algorithms](#sorting-algorithms)
- [File Input/Output](#file-inputoutput)
- [Tests](#tests)
- [Contributing](#contributing)
- [License](#license)

## Overview

The algorithms use temporary files to sort inputs larger than available memory. The visual demonstration records events from those same implementations for a fixed set of 24 integers.

## Installation

Use JDK 22 and Maven.

1. Clone the repository to your local machine.
   ```bash
   git clone https://github.com/MauricioART/External-Sorting-Algorithms.git
   cd External-Sorting-Algorithms
   ```

2. **Run the application**
   ```bash
   mvn javafx:run
   ```

On Windows, you can also use `.\mvnw.cmd javafx:run` from PowerShell.

## Usage

### Visual demonstration

Choose an algorithm and click **Reproducir**. Each bar represents one value and keeps its height as it moves between files or radix buckets. Red marks values involved in the current comparison or write. Green marks the completed output. The label above the chart identifies the current operation, and the counters track merge comparisons and writes during the demonstration.

| Control | Action |
| --- | --- |
| **Reproducir / Pausar** | Start or pause playback. |
| **Anterior / Siguiente** | Move backward or forward one event. |
| **Reiniciar** | Return to the original input order. |
| **Velocidad** | Choose a playback speed from 0.5× to 4×. |
| **Sonido** | Play a short note for the active value. Audio starts disabled. |

### Sort a file

Select an algorithm and click **Ordenar archivo…** to process a text file in the background. The window displays a copyable path to the sorted result. The chart continues to use the 24-value demonstration. For files, Polyphase uses blocks of 8,192 integers. Its demonstration uses blocks of four.

## Sorting Algorithms
- **Polyphase Merge Sort** sorts fixed-size chunks in memory, distributes them across two temporary files using Fibonacci run counts, and rotates the third file into the input pair after each merge phase.

- **Balanced Merge Sort** detects consecutive runs that are already in ascending order. It alternates those runs between two files, merges pairs of runs, and swaps input and output files between passes.

- **External Radix Sort** distributes values into temporary files for signed digits from −9 to 9. It collects the buckets in order and repeats from the least significant digit to the most significant digit present in the input.

## File Input/Output
Choose a UTF-8 text file containing signed 32-bit integers separated by commas or whitespace. For example, `3,-1,2` sorts to `-1,2,3,`.

The input can include duplicate values, negative values, blank lines, and a trailing comma. An empty input produces an empty output. An invalid integer stops the sort and reports its position.

Each successful run writes a new `<algorithm>-sorted-<id>.txt` next to the input. The original stays intact. The directory must be writable so the algorithm can create temporary files and publish the result.

## Tests

```bash
mvn test
```

The tests check all three sorters against an in-memory reference, exercise malformed input and concurrent runs, and verify that the animation preserves every value until it reaches the sorted output.

## Contributing
If you would like to contribute to this project, feel free to fork the repository and submit a pull request. Contributions in the form of bug fixes, new features, and code improvements are welcome.

## License
This project is licensed under the MIT License. See the LICENSE file for details.
