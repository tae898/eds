package sample

import (
	"fmt"
	lane "github.com/geogro/lane"
	"math/rand"
	"time"
)

type method string

const (
	AggarwalFixed    method = "AggarwalFixed"
	AggarwalVariable method = "AggarwalVariable"
	EDS              method = "EDS"
	EDSX             method = "EDSX"
	EDSP             method = "EDSP"
)

type (
	Reservoir struct {
		//Basic Resevoir information
		Data   []string
		Id     uint64
		Perc   float64
		Method method
		Rnd    *rand.Rand

		//Settings for sampling
		Gamma float64
		Pin   float64

		//specific: AggarwalVariable
		pinVar float64
		nmax   float64

		//specific: EDS
		collection *lane.PQueue
		x          float64
	}
)

func NewReservoir(Gamma float64, Pin float64, m method) *Reservoir {
	var res Reservoir
	res.Data = make([]string, int(1/Gamma))
	res.Gamma = Gamma
	res.Pin = Pin
	res.Method = m
	res.Rnd = rand.New(rand.NewSource(time.Now().UnixNano()))

	res.pinVar = 1       //only used for AggarwalVariable
	res.nmax = 1 / Gamma //only used for AggarwalVariable

	// Some items and their priorities.
	//items := map[string]int{}

	res.x = 0.6
	res.collection = lane.NewPQueue(lane.MINPQ)

	//res.collection = make(PriorityQueue, len(items))
	//heap.Init(&res.collection)
	//res.collection = &pq

	return &res
}

func (r *Reservoir) Sample(e []byte) {
	switch r.Method {
	case AggarwalFixed:
		r.aggarwalFixed(e)
		break
	case AggarwalVariable:
		r.aggarwalVariable(e)
		break
	case EDS:
		r.eds(e)
		break
	case EDSX:
		r.edsX(e)
		break
	case EDSP:
		r.edsP(e)
		break
	}
}

func (r *Reservoir) aggarwalFixed(e []byte) {
	//flip coin for possible input to resevoir
	if float64(r.Rnd.Intn(100)) < r.Pin*100 {
		r.Perc = float64(r.Id) / float64(len(r.Data))
		if r.Perc < 1 {
			//fmt.Printf("Reservoir full %f percent \n", r.Perc)
		}

		//replace
		if float64(r.Rnd.Intn(100)) < r.Perc*100 {
			p := r.Rnd.Intn(len(r.Data) - 1)
			r.Data[p] = string(e)
			//r.Id++
		} else { // append
			r.Data[r.Id] = string(e)
			r.Id++
		}
	}
}

func (r *Reservoir) aggarwalVariable(e []byte) {
	//flip coin for possible input to resevoir
	if float64(r.Rnd.Intn(100)) < r.pinVar*100 {
		//reducing input probability, unless Pin is reached
		r.pinVar = r.pinVar - 1/r.nmax
		if r.pinVar < r.Pin {
			r.pinVar = r.Pin
		}

		r.Perc = float64(r.Id) / float64(len(r.Data))
		if r.Perc < 1 {
			//fmt.Printf("Reservoir full %f percent \n", r.Perc)
		}

		//replace
		if float64(r.Rnd.Intn(100)) < r.Perc*100 {
			p := rand.Intn(len(r.Data) - 1)
			r.Data[p] = string(e)
			//r.Id++
		} else { // append
			r.Data[r.Id] = string(e)
			r.Id++
		}
	}
}

func (r *Reservoir) eds(e []byte) {
	r.collection.Push(string(e), r.pinVar*r.Rnd.Float64())

	headVal, headPriority := r.collection.Head()
	for headVal != nil && headPriority < r.x {
		fmt.Printf("Remove item with priority %f given threshold %f \n", headPriority, r.x)
		r.collection.Pop()
		headVal, headPriority = r.collection.Head()
	}

	r.x = r.x / 0.9999
	r.pinVar = r.pinVar / 0.9999

	r.Perc = float64(r.collection.Size()) / (1 / r.Gamma)
}

func (r *Reservoir) edsX(e []byte) {
	r.collection.Push(string(e), r.pinVar*r.Rnd.Float64())

	headVal, headPriority := r.collection.Head()
	for headVal != nil && headPriority < r.x {
		fmt.Printf("Remove item with priority %f given threshold %f \n", headPriority, r.x)
		r.collection.Pop()
		headVal, headPriority = r.collection.Head()
	}

	r.x = r.x / 0.9999
	r.pinVar = r.pinVar / 0.9999

	//r.Perc = float64(r.collection.Len()) / (1 / r.Gamma)
	r.Perc = r.x
}

func (r *Reservoir) edsP(e []byte) {
	r.collection.Push(string(e), r.pinVar*r.Rnd.Float64())

	headVal, headPriority := r.collection.Head()
	for headVal != nil && headPriority < r.x {
		fmt.Printf("Remove item with priority %f given threshold %f \n", headPriority, r.x)
		r.collection.Pop()
		headVal, headPriority = r.collection.Head()
	}

	r.x = r.x / 0.9999
	r.pinVar = r.pinVar / 0.9999

	//r.Perc = float64(r.collection.Len()) / (1 / r.Gamma)
	r.Perc = r.pinVar
}
