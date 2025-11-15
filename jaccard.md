# Explanation of Jaccard Index

Jaccard indexes are rather simple to explain. Essentially, it is a number which quantifies how similar two sets are to one another. For given sets A and B, it is calculated by dividing the cardinality (size) of the intersection (shared elements) of A and B by the cardinality of the union (all elements in either set) of A and B.

## Example

A: [2, 4, 6, 8, 10, 12, 14, 16, 18, 20]

B: [1, 2, 3, 5, 8, 13, 21, 34, 55, 89]

C: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]

Intersection of A and B: [2, 8] (Cardinality is 2)

Union of A and B: [1, 2, 3, 4, 5, 6, 8, 10, 12, 13, 14, 16, 18, 20, 21, 24, 55, 89] (Cardinality is 18)

So the Jaccard index of A and B is 0.111 repeating.

Intersection of A and C: [2, 4, 6, 8, 10] (Cardinality is 5)

Union of A and C: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 14, 16, 18, 20] (Cardinality is 15)

So the Jaccard index of A and C is 0.333 repeating.
