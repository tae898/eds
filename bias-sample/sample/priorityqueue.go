package sample

import "container/heap"

type (
	Item struct {
		value    string
		priority float64
		index    int
	}
	PriorityQueue []*Item
)

func (pq PriorityQueue) Len() int { return len(pq) }

func (pq PriorityQueue) Less(i, j int) bool {
	// We want Pop to give us the highest, not lowest, priority so we use greater than here.
	return pq[i].priority < pq[j].priority
}

func (pq PriorityQueue) Swap(i, j int) {
	pq[i], pq[j] = pq[j], pq[i]
	pq[i].index = i
	pq[j].index = j
}

func (pq *PriorityQueue) Push(x interface{}) {
	n := len(*pq)
	item := x.(*Item)
	item.index = n
	*pq = append(*pq, item)
}

func (pq *PriorityQueue) Peek() interface{} {
	old := *pq
	n := len(old)

	if n > 0 {
		item := *old[n-1]
		return item
	} else {
		return nil
	}
}

func (pq *PriorityQueue) Pop() interface{} {
	old := *pq
	n := len(old)

	if n > 0 {
		item := old[n-1]
		item.index = -1 // for safety
		*pq = old[0 : n-1]
		return item
	} else {
		return nil
	}

}

// update modifies the priority and value of an Item in the queue.
func (pq *PriorityQueue) update(item *Item, value string, priority float64) {
	item.value = value
	item.priority = priority
	heap.Fix(pq, item.index)
}
