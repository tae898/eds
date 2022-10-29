package stream

import "math/rand"

type Stream struct {
	data []byte
	id   uint64
}

const letterBytes = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"

func (s *Stream) Read() []byte {
	s.id++
	d := RandStringBytes(1)
	s.data = append(s.data, d[:]...)
	return d
}

func (s *Stream) ReadAll() []byte {
	return s.data
}

func (s *Stream) Count() uint64 {
	return s.id
}

func RandStringBytes(n int) []byte {
	b := make([]byte, n)
	for i := range b {
		b[i] = letterBytes[rand.Intn(len(letterBytes))]
	}
	return b
}
