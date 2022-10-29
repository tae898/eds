package main

import (
	"fmt"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/wcharczuk/go-chart"
	"gitlab.comtip.net/georg/bias-sample/sample"
	"gitlab.comtip.net/georg/bias-sample/stream"
)

type (
	plotData struct {
		X []float64
		Y []float64
	}
	container struct {
		reservoir *sample.Reservoir
		plot      plotData
	}
)

var list map[string]*container
var data stream.Stream

func main() {
	//Quit with SIGINT and render plot
	c := make(chan os.Signal, 2)
	signal.Notify(c, os.Interrupt, syscall.SIGTERM)
	go func() {
		<-c

		graph := chart.Chart{
			XAxis: chart.XAxis{
				Style: chart.Style{
					Show: true, //enables / displays the x-axis
				},
			},
			YAxis: chart.YAxis{
				Style: chart.Style{
					Show: true, //enables / displays the y-axis
				},
			},
			Background: chart.Style{
				Padding: chart.Box{
					Top:  20,
					Left: 20,
				},
			},
		}

		var series []chart.Series
		for _, pd := range list {
			series = append(series, chart.ContinuousSeries{
				Name:    string(pd.reservoir.Method),
				XValues: pd.plot.X,
				YValues: pd.plot.Y,
			})
		}
		graph.Series = series
		graph.Elements = []chart.Renderable{
			chart.Legend(&graph),
		}

		file, _ := os.Create("chart.svg")
		defer file.Close()

		_ = graph.Render(chart.SVG, file)
		fmt.Println("Quit")
		os.Exit(1)
	}()

	list = make(map[string]*container)
	fmt.Println("Starting stream")

	//creating Aggawal sampling method

	list["aggaFix1"] = &container{
		sample.NewReservoir(0.001, 1, sample.AggarwalFixed),
		plotData{},
	}
	list["aggaFix2"] = &container{
		sample.NewReservoir(0.001, 0.5, sample.AggarwalFixed),
		plotData{},
	}

	list["aggaVar"] = &container{
		sample.NewReservoir(0.001, 0.5, sample.AggarwalVariable),
		plotData{},
	}

	list["eds"] = &container{
		sample.NewReservoir(0.001, 0.5, sample.EDS),
		plotData{},
	}
	/*
		list["eds2"] = &container{
			sample.NewReservoir(0.001, 0.5, sample.EDSX),
			plotData{},
		}

		list["eds3"] = &container{
			sample.NewReservoir(0.001, 0.5, sample.EDSP),
			plotData{},
		}
	*/

	for {
		//retrieve new data point
		d := data.Read()

		//perform sampling
		for _, s := range list {
			s.reservoir.Sample(d)

			s.plot.X = append(s.plot.X, float64(data.Count()))
			s.plot.Y = append(s.plot.Y, s.reservoir.Perc)
		}
		time.Sleep(5 * time.Millisecond)
	}

}
