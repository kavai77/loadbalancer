package com.himadri

class MaximumNumberOfProviderException() : Exception("Reached the maximum number of providers")
class NoAvailableProviderException() : Exception("No available provider found")
class RequestThrottlingException() : Exception("The request have been throttled")