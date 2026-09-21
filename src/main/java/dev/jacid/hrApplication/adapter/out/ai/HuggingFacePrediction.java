package dev.jacid.hrApplication.adapter.out.ai;

/** One label/score pair as returned by the Hugging Face text-classification inference API. */
record HuggingFacePrediction(String label, Double score) {}
