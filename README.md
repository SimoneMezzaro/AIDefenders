# AI Defenders

AI Defenders is a smart assistant created as an integration for the Code Defenders mutation testing game. It
leverages GPT models and chat completion API by OpenAI to assist students playing Code Defenders.

AI Defenders has been developed to study how software testing students interact with GPT-based tools and to explore the impact
of Large Language Models on their learning and practice of software testing. The findings of this study were
presented at the LEARNER workshop run in the context of the EASE 2024 conference.

For more information, refer to the paper:
> [An Empirical Study on How Large Language Models Impact Software Testing Learning](https://dl.acm.org/doi/10.1145/3661167.3661273)

## How to run AI Defenders

An instance of Code Defenders, featuring the AI Defenders assistant, can be easily deployed locally using Docker.

### Configuration

Since AI Defenders relies on OpenAI API to provide its assistance, it requires to specify an API key and the
GPT model to use. Both configuration properties should be set in a `.env` file created in the `/docker/` folder. A
template for this file is available in the [`example.env` file](docker/example.env).

The API token can be retrieved from the [OpenAI website](https://platform.openai.com/api-keys) and it can be
set as 
```properties
CODEDEFENDERS_OPENAI_API_KEY = sk-XXXXXX_XXXXXX
```

The GPT model can be selected among the [currently available models](https://platform.openai.com/docs/models) and it can be set as 
```properties
CODEDEFENDERS_OPENAI_CHATGPT_MODEL = gpt-3.5-turbo
```

Refer to the [original Docker documentation](docker/README.md) of Code Defenders for editing additional properties in
the `.env` file.

### Deployment

After setting up the `.env` file, deploy the platform by navigating to the root of the AI Defenders repository and 
running the following commands:
```sh
docker build --file ./docker/Dockerfile --tag localhost/aidefenders:aid .
cd docker
docker-compose up
```
